package com.team3.deokhugam.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.exception.book.BookAlreadyExistsException;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.repository.book.BookRepository;
import java.time.LocalDate;
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
}
