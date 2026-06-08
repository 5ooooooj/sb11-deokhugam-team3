package com.team3.deokhugam.service.book;

import com.team3.deokhugam.client.naver.NaverBookClient;
import com.team3.deokhugam.client.naver.dto.NaverBookItemDto;
import com.team3.deokhugam.client.naver.dto.NaverBookSearchDto;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookCursor;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.dto.book.BookInfoDto;
import com.team3.deokhugam.dto.book.BookOrderBy;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.dto.book.BookUpdateRequest;
import com.team3.deokhugam.exception.book.BookAlreadyExistsException;
import com.team3.deokhugam.exception.book.BookInfoNotFoundException;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.exception.book.InvalidBookIsbnException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.service.s3.S3Service;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

  // -과 공백 제거 후 ISBN-10 or 13 허용, ISBN-10 은 맨뒤 X 허용
  private static final Pattern ISBN_PATTERN = Pattern.compile("^(\\d{9}[\\dX]|\\d{13})$");

  // Naver pubdate는 yyyyMMdd 형식이므로 ISO_DATE 사용
  private static final DateTimeFormatter NAVER_PUBLISHED_DATE_FORMATTER =
      DateTimeFormatter.BASIC_ISO_DATE;

  private final BookRepository bookRepository;

  private final S3Service s3Service;

  private final NaverBookClient naverBookClient;

  @Transactional
  public BookDto create(UUID requestUserId, BookCreateRequest request,
      MultipartFile thumbnailImage) {
    if (request.isbn() != null && bookRepository.existsByIsbn(request.isbn())) {
      throw new BookAlreadyExistsException();
    }

    String thumbnailUrl = uploadThumbnailIfPresent(
        thumbnailImage,
        request.thumbnailUrl(),
        requestUserId
    );

    Book book =
        new Book(
            requestUserId,
            request.title(),
            request.author(),
            request.description(),
            request.publisher(),
            request.publishedDate(),
            request.isbn(),
            thumbnailUrl
        );
    Book savedBook = bookRepository.save(book);

    return BookDto.from(savedBook);
  }

  public BookDto findById(UUID bookId) {
    Book book = getActiveBook(bookId);

    return BookDto.from(book);
  }

  public BookInfoDto findBookInfoByIsbn(String isbn) {
    String normalizedIsbn = normalizeIsbn(isbn);
    NaverBookSearchDto response = naverBookClient.searchByIsbn(normalizedIsbn);

    if (response == null || response.hasNoItems()) {
      throw new BookInfoNotFoundException();
    }

    NaverBookItemDto item = response.items().stream()
        .filter(bookItem -> containsIsbn(bookItem, normalizedIsbn))
        .findFirst()
        .orElseThrow(BookInfoNotFoundException::new);

    return toBookInfoDto(item, normalizedIsbn);
  }

  public CursorPageResponse<BookDto> search(BookSearchRequest request) {
    BookSearchRequest pageRequest = request.withLimit(request.limit() + 1);
    List<Book> books = bookRepository.search(pageRequest);

    boolean hasNext = books.size() > request.limit();

    List<Book> pageBooks = hasNext ? books.subList(0, request.limit()) : books;

    long totalElements = bookRepository.count(request);

    List<BookDto> content = pageBooks.stream()
        .map(BookDto::from)
        .toList();

    Book lastBook = pageBooks.isEmpty() ? null : pageBooks.get(pageBooks.size() - 1);

    return new CursorPageResponse<>(
        content,
        resolveNextCursor(lastBook, request.orderBy()),
        lastBook == null ? null : lastBook.getCreatedAt(),
        content.size(),
        totalElements,
        hasNext
    );
  }

  @Transactional
  public BookDto update(UUID bookId, UUID requestUserId, BookUpdateRequest request,
      MultipartFile thumbnailImage) {
    Book book = getActiveBook(bookId);
    book.validateOwner(requestUserId);

    String thumbnailUrl = uploadThumbnailIfPresent(
        thumbnailImage,
        request.thumbnailUrl(),
        requestUserId
    );

    book.update(
        request.title(),
        request.author(),
        request.description(),
        request.publisher(),
        request.publishedDate(),
        thumbnailUrl
    );

    return BookDto.from(book);
  }

  @Transactional
  public void delete(UUID bookId, UUID requestUserId) {
    Book book = getActiveBook(bookId);
    book.validateOwner(requestUserId);

    book.softDelete();
  }

  @Transactional
  public void hardDelete(UUID bookId, UUID requestUserId) {
    Book book = bookRepository.findById(bookId).orElseThrow(BookNotFoundException::new);
    book.validateOwner(requestUserId);

    bookRepository.delete(book);
  }

  private Book getActiveBook(UUID bookId) {
    return bookRepository.findByIdAndDeletedAtIsNull(bookId)
        .orElseThrow(BookNotFoundException::new);
  }

  private String resolveNextCursor(Book book, BookOrderBy orderBy) {
    if (book == null) {
      return null;
    }

    String cursorValue = resolveCursorValue(book, orderBy);

    if (cursorValue == null || book.getCreatedAt() == null || book.getId() == null) {
      return null;
    }

    return BookCursor.encode(cursorValue, book.getCreatedAt(), book.getId());
  }

  private String resolveCursorValue(Book book, BookOrderBy orderBy) {
    return switch (orderBy) {
      case TITLE -> book.getTitle();
      case PUBLISHED_DATE ->
          book.getPublishedDate() == null ? null : book.getPublishedDate().toString();
      case RATING -> book.getRating() == null ? null : book.getRating().toString();
      case REVIEW_COUNT -> String.valueOf(book.getReviewCount());
    };
  }

  private String normalizeIsbn(String isbn) {
    if (isbn == null) {
      throw new InvalidBookIsbnException();
    }

    String normalizedIsbn = isbn.replaceAll("[-\\s]", "")
        .toUpperCase(Locale.ROOT);

    if (!ISBN_PATTERN.matcher(normalizedIsbn).matches()) {
      throw new InvalidBookIsbnException();
    }

    return normalizedIsbn;
  }

  private boolean containsIsbn(NaverBookItemDto item, String normalizedIsbn) {
    if (item == null || item.isbn() == null) {
      return false;
    }

    String normalizedNaverIsbn = item.isbn()
        .replaceAll("[-\\s]", "")
        .toUpperCase(Locale.ROOT);

    return normalizedNaverIsbn.contains(normalizedIsbn);
  }

  private BookInfoDto toBookInfoDto(NaverBookItemDto item, String normalizedIsbn) {
    return new BookInfoDto(
        cleanHtml(item.title()),
        cleanHtml(item.author()),
        cleanHtml(item.description()),
        cleanHtml(item.publisher()),
        parsePublishedDate(item.pubdate()),
        normalizedIsbn,
        item.image()
    );
  }

  private String cleanHtml(String value) {
    if (value == null) {
      return null;
    }

    return HtmlUtils.htmlUnescape(value.replaceAll("<[^>]*>", "")).trim();
  }

  private LocalDate parsePublishedDate(String pubdate) {
    if (pubdate == null || pubdate.isBlank()) {
      return null;
    }

    try {
      return LocalDate.parse(pubdate, NAVER_PUBLISHED_DATE_FORMATTER);
    } catch (DateTimeParseException e) {
      return null;
    }
  }

  private String uploadThumbnailIfPresent(
      MultipartFile thumbnailImage,
      String fallbackThumbnailUrl,
      UUID requestUserId
  ) {
    if (thumbnailImage == null || thumbnailImage.isEmpty()) {
      return fallbackThumbnailUrl;
    }

    String key = genarateThumbnailKey(requestUserId, thumbnailImage.getOriginalFilename());
    String uploadedUrl = s3Service.upload(thumbnailImage, key);

    registerS3RollbackCleanup(key);

    return uploadedUrl;
  }

  private String genarateThumbnailKey(UUID requestUserId, String originalFilename) {
    String safeOriginalFilename =
        originalFilename == null || originalFilename.isBlank()
            ? "thumbnail"
            : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");

    return "books/" + requestUserId + "/" + UUID.randomUUID() + "_" + safeOriginalFilename;
  }

  private void registerS3RollbackCleanup(String key) {
    if (!TransactionSynchronizationManager.isSynchronizationActive()) {
      return;
    }

    TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
      @Override
      public void afterCompletion(int status) {
        if (status != STATUS_ROLLED_BACK) {
          return;
        }

        try {
          s3Service.delete(key);
        } catch (RuntimeException e) {
          log.warn("트랜잭션 롤백 후 S3 업로드 보상 삭제 실패 - key : {}", key, e);
        }
      }
    });
  }
}
