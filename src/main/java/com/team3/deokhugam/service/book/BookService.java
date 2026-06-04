package com.team3.deokhugam.service.book;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookCursor;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.dto.book.BookOrderBy;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.dto.book.BookUpdateRequest;
import com.team3.deokhugam.exception.book.BookAlreadyExistsException;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.service.s3.S3Service;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

  private final BookRepository bookRepository;

  private final S3Service s3Service;

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

  private String uploadThumbnailIfPresent(
      MultipartFile thumbnailImage,
      String fallbackThumbnailUrl,
      UUID requestUserId
  ) {
    if (thumbnailImage == null || thumbnailImage.isEmpty()) {
      return fallbackThumbnailUrl;
    }

    String key = genarateThumbnailKey(requestUserId, thumbnailImage.getOriginalFilename());

    return s3Service.upload(thumbnailImage, key);
  }

  private String genarateThumbnailKey(UUID requestUserId, String originalFilename) {
    String safeOriginalFilename =
        originalFilename == null || originalFilename.isBlank()
            ? "thumbnail"
            : originalFilename.replace("[^a-zA-Z0-9._-]","_");

    return "books/" + requestUserId + "/" + UUID.randomUUID() + "_" + safeOriginalFilename;
  }
}
