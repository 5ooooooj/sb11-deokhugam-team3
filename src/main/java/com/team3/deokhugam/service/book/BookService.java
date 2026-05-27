package com.team3.deokhugam.service.book;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.dto.book.BookOrderBy;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.exception.book.BookAlreadyExistsException;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.book.BookRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookService {

  private final BookRepository bookRepository;

  @Transactional
  public BookDto create(BookCreateRequest request) {
    if (request.isbn() != null && bookRepository.existsByIsbn(request.isbn())) {
      throw new BookAlreadyExistsException();
    }

    Book book =
        new Book(
            request.title(),
            request.author(),
            request.description(),
            request.publisher(),
            request.publishedDate(),
            request.isbn(),
            request.thumbnailUrl()
        );
    Book savedBook = bookRepository.save(book);

    return BookDto.from(savedBook);
  }

  public BookDto findById(UUID bookId) {
    Book book = bookRepository.findById(bookId)
        .orElseThrow(BookNotFoundException::new);

    return BookDto.from(book);
  }

  public CursorPageResponse<BookDto> search(BookSearchRequest request) {
    List<Book> books = bookRepository.search(request);
    long totalElements = bookRepository.count(request);

    List<BookDto> content = books.stream()
        .map(BookDto::from)
        .toList();

    Book lastBook = books.isEmpty() ? null : books.get(books.size() - 1);

    return new CursorPageResponse<>(
        content,
        resolveNextCursor(lastBook, request.orderBy()),
        lastBook == null ? null : lastBook.getCreatedAt(),
        content.size(),
        totalElements,
        totalElements > content.size()
    );
  }

  private String resolveNextCursor(Book book, BookOrderBy orderBy) {
    if (book == null) {
      return null;
    }

    return switch (orderBy) {
      case TITLE ->  book.getTitle();
      case PUBLISHED_DATE ->   book.getPublishedDate().toString();
      case RATING ->   book.getRating().toString();
      case REVIEW_COUNT -> String.valueOf(book.getReviewCount());
    };
  }
}
