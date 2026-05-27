package com.team3.deokhugam.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.exception.book.BookAlreadyExistsException;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.book.BookRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

  @Mock
  private BookRepository bookRepository;

  @InjectMocks
  private BookService bookService;

  @Test
  @DisplayName("도서 생성")
  void createBook() {
    // given
    BookCreateRequest request =
        new BookCreateRequest(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            "https://example.com/book.jpg"
        );

    Book savedBook =
        new Book(
            request.title(),
            request.author(),
            request.description(),
            request.publisher(),
            request.publishedDate(),
            request.isbn(),
            request.thumbnailUrl()
        );

    when(bookRepository.save(any(Book.class))).thenReturn(savedBook);

    // when
    BookDto result = bookService.create(request);

    //then
    assertThat(result.title()).isEqualTo(request.title());
    assertThat(result.author()).isEqualTo(request.author());
    assertThat(result.isbn()).isEqualTo(request.isbn());
  }

  @Test
  @DisplayName("ISBN 중복되면 도서 생성 불가")
  void createBookWithDuplicateISbn() {
    // given
    BookCreateRequest request =
        new BookCreateRequest(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            "https://example.com/book.jpg"
        );

    when(bookRepository.existsByIsbn(request.isbn())).thenReturn(true);

    // when, then
    assertThatThrownBy(() -> bookService.create(request))
        .isInstanceOf(BookAlreadyExistsException.class);
  }

  @Test
  @DisplayName("도서 ID로 상세 조회하면 BookDto 반환")
  void findById() {
    // given
    UUID bookId = UUID.randomUUID();

    Book book =
        new Book(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            "https://example.com/book.jpg"
        );

    ReflectionTestUtils.setField(book, "id", bookId);

    when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

    // when
    BookDto result = bookService.findById(bookId);

    // then
    assertThat(result.id()).isEqualTo(bookId);
    assertThat(result.title()).isEqualTo(book.getTitle());
    assertThat(result.author()).isEqualTo(book.getAuthor());
    assertThat(result.isbn()).isEqualTo(book.getIsbn());
  }

  @Test
  @DisplayName("존재하지 않는 도서 ID로 상세 조회하면 예외 발생")
  void findByIdWithNotFound() {
    // given
    UUID bookId = UUID.randomUUID();

    when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> bookService.findById(bookId))
        .isInstanceOf(BookNotFoundException.class);
  }

  @Test
  @DisplayName("도서 목록을 조회하면 커서 페이지 응답을 반환")
  void searchBooks() {
    // given
    BookSearchRequest request =
        BookSearchRequest.of(
            "자바",
            "title",
            "ASC",
            null,
            null,
            2
        );

    Instant firstCreatedAt = Instant.parse("2026-05-27T00:00:00Z");
    Instant secondCreatedAt = Instant.parse("2026-05-27T00:01:00Z");

    Book firstBook =
        book(
            UUID.randomUUID(),
            "코드잇 스프링",
            "강우진",
            "스프링백엔드",
            "테스트출판사",
            LocalDate.of(2026, 1, 1),
            "9780000002101",
            "https://example.com/codeit-spring.jpg",
            firstCreatedAt
        );

    Book secondBook =
        book(
            UUID.randomUUID(),
            "코드잇 스프링2",
            "강우진2",
            "스프링백엔드2",
            "테스트출판사",
            LocalDate.of(2026, 1, 2),
            "9780000002102",
            "https://example.com/codeit-spring.jpg",
            secondCreatedAt
        );

    when(bookRepository.search(request)).thenReturn(List.of(firstBook, secondBook));
    when(bookRepository.count(request)).thenReturn(3L);

    // when
    CursorPageResponse<BookDto> result = bookService.search(request);

    // then
    assertThat(result.content()).hasSize(2);
    assertThat(result.content())
        .extracting(BookDto::title)
        .containsExactly("코드잇 스프링", "코드잇 스프링2");
    assertThat(result.nextCursor()).isEqualTo("코드잇 스프링2");
    assertThat(result.nextAfter()).isEqualTo(secondCreatedAt);
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.totalElements()).isEqualTo(3L);
    assertThat(result.hasNext()).isTrue();

    verify(bookRepository).search(request);
    verify(bookRepository).count(request);
  }

  @Test
  @DisplayName("도서 목록 조회 결과가 비어있으면 빈 커서 페이지 응답을 반환")
  void searchBooksWithEmptyResult() {
    // given
    BookSearchRequest request =
        BookSearchRequest.of(
            "없는 책",
            "title",
            "ASC",
            null,
            null,
            10
        );

    when(bookRepository.search(request)).thenReturn(List.of());
    when(bookRepository.count(request)).thenReturn(0L);

    // when
    CursorPageResponse<BookDto> result = bookService.search(request);

    // then
    assertThat(result.content()).isEmpty();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.nextAfter()).isNull();
    assertThat(result.size()).isZero();
    assertThat(result.totalElements()).isZero();
    assertThat(result.hasNext()).isFalse();

    verify(bookRepository).search(request);
    verify(bookRepository).count(request);
  }

  private Book book(
      UUID id,
      String title,
      String author,
      String description,
      String publisher,
      LocalDate publicationDate,
      String isbn,
      String thumbnailUrl,
      Instant createdAt
  ) {
    Book book =
        new Book(
            title,
            author,
            description,
            publisher,
            publicationDate,
            isbn,
            thumbnailUrl
        );

    ReflectionTestUtils.setField(book, "id", id);
    ReflectionTestUtils.setField(book, "createdAt", createdAt);
    ReflectionTestUtils.setField(book, "updatedAt", createdAt);

    return book;
  }
}
