package com.team3.deokhugam.service.book;

import static com.team3.deokhugam.domain.book.BookTestFactory.book;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookCursor;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.dto.book.BookOrderBy;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.dto.book.BookUpdateRequest;
import com.team3.deokhugam.exception.book.BookAlreadyExistsException;
import com.team3.deokhugam.exception.book.BookForbiddenException;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.service.s3.S3Service;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@ExtendWith(MockitoExtension.class)
class BookServiceTest {

  @Mock
  private BookRepository bookRepository;

  @Mock
  private S3Service s3Service;

  @InjectMocks
  private BookService bookService;

  @Test
  @DisplayName("도서 생성")
  void createBook() {
    // given
    UUID requestUserId = UUID.randomUUID();

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

    when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    BookDto result = bookService.create(requestUserId, request, null);

    // then
    assertThat(result.title()).isEqualTo(request.title());
    assertThat(result.author()).isEqualTo(request.author());
    assertThat(result.isbn()).isEqualTo(request.isbn());
    assertThat(result.thumbnailUrl()).isEqualTo(request.thumbnailUrl());

    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
    verify(bookRepository).save(bookCaptor.capture());

    Book savedBook = bookCaptor.getValue();
    assertThat(savedBook.getUserId()).isEqualTo(requestUserId);
    assertThat(savedBook.getTitle()).isEqualTo(request.title());
    assertThat(savedBook.getAuthor()).isEqualTo(request.author());
    assertThat(savedBook.getIsbn()).isEqualTo(request.isbn());
    assertThat(savedBook.getThumbnailUrl()).isEqualTo(request.thumbnailUrl());

    verify(s3Service, never()).upload(any(MultipartFile.class), any(String.class));
  }

  @Test
  @DisplayName("썸네일 이미지가 있으면 S3에 업로드하고 업로드 URL을 저장")
  void createBookWithThumbnailImage() {
    // given
    UUID requestUserId = UUID.randomUUID();

    BookCreateRequest request =
        new BookCreateRequest(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            null
        );

    MultipartFile thumbnailImage =
        new MockMultipartFile(
            "thumbnailImage",
            "thumbnail.jpg",
            "image/jpeg",
            "test-image".getBytes()
        );

    String uploadedUrl = "https://example.com/uploaded-thumbnail.jpg";
    when(s3Service.upload(any(MultipartFile.class), any(String.class))).thenReturn(uploadedUrl);
    when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    BookDto result = bookService.create(requestUserId, request, thumbnailImage);

    // then
    assertThat(result.thumbnailUrl()).isEqualTo(uploadedUrl);

    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
    verify(bookRepository).save(bookCaptor.capture());

    Book savedBook = bookCaptor.getValue();
    assertThat(savedBook.getThumbnailUrl()).isEqualTo(uploadedUrl);

    verify(s3Service).upload(any(MultipartFile.class), any(String.class));
  }

  @Test
  @DisplayName("ISBN 중복되면 도서 생성 불가")
  void createBookWithDuplicateIsbn() {
    // given
    UUID requestUserId = UUID.randomUUID();

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
    assertThatThrownBy(() -> bookService.create(requestUserId, request, null))
        .isInstanceOf(BookAlreadyExistsException.class);

    verify(bookRepository, never()).save(any(Book.class));
    verify(s3Service, never()).upload(any(MultipartFile.class), any(String.class));
  }

  @Test
  @DisplayName("도서 ID로 상세 조회하면 BookDto 반환")
  void findById() {
    // given
    UUID bookId = UUID.randomUUID();

    Book book = book()
        .id(bookId)
        .title("그리고 아무도 없었다")
        .author("애거서 크리스티")
        .description("외딴 섬에서 벌어지는 연쇄 살인 사건")
        .publisher("황금가지")
        .publishedDate(LocalDate.of(2013, 12, 31))
        .isbn("9788960177758")
        .thumbnailUrl("https://example.com/book.jpg")
        .build();

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.of(book));

    // when
    BookDto result = bookService.findById(bookId);

    // then
    assertThat(result.id()).isEqualTo(bookId);
    assertThat(result.title()).isEqualTo(book.getTitle());
    assertThat(result.author()).isEqualTo(book.getAuthor());
    assertThat(result.isbn()).isEqualTo(book.getIsbn());

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
  }

  @Test
  @DisplayName("존재하지 않는 도서 ID로 상세 조회하면 예외 발생")
  void findByIdWithNotFound() {
    // given
    UUID bookId = UUID.randomUUID();

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> bookService.findById(bookId))
        .isInstanceOf(BookNotFoundException.class);

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
  }

  @Test
  @DisplayName("도서 목록을 조회하면 커서 페이지 응답을 반환")
  void searchBooks() {
    // given
    BookSearchRequest request =
        BookSearchRequest.of(
            "자바",
            BookOrderBy.TITLE,
            Sort.Direction.ASC,
            null,
            2
        );

    Instant firstCreatedAt = Instant.parse("2026-05-27T00:00:00Z");
    Instant secondCreatedAt = Instant.parse("2026-05-27T00:01:00Z");
    Instant thirdCreatedAt = Instant.parse("2026-05-27T00:02:00Z");

    Book firstBook = book()
        .id(UUID.randomUUID())
        .title("코드잇 스프링")
        .author("강우진")
        .description("스프링백엔드")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9780000002101")
        .thumbnailUrl("https://example.com/codeit-spring.jpg")
        .createdAt(firstCreatedAt)
        .build();

    Book secondBook = book()
        .id(UUID.randomUUID())
        .title("코드잇 스프링2")
        .author("강우진2")
        .description("스프링백엔드2")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 2))
        .isbn("9780000002102")
        .thumbnailUrl("https://example.com/codeit-spring2.jpg")
        .createdAt(secondCreatedAt)
        .build();

    Book thirdBook = book()
        .id(UUID.randomUUID())
        .title("코드잇 스프링3")
        .author("강우진3")
        .description("스프링백엔드3")
        .publisher("테스트출판사")
        .publishedDate(LocalDate.of(2026, 1, 3))
        .isbn("9780000002103")
        .thumbnailUrl("https://example.com/codeit-spring3.jpg")
        .createdAt(thirdCreatedAt)
        .build();

    BookSearchRequest pageRequest = request.withLimit(request.limit() + 1);

    when(bookRepository.search(pageRequest)).thenReturn(List.of(firstBook, secondBook, thirdBook));
    when(bookRepository.count(request)).thenReturn(3L);

    // when
    CursorPageResponse<BookDto> result = bookService.search(request);

    // then
    assertThat(result.content()).hasSize(2);
    assertThat(result.content())
        .extracting(BookDto::title)
        .containsExactly("코드잇 스프링", "코드잇 스프링2");
    assertThat(result.nextCursor()).isNotNull();
    BookCursor nextCursor = BookCursor.decode(result.nextCursor());
    assertThat(nextCursor.value()).isEqualTo("코드잇 스프링2");
    assertThat(nextCursor.createdAt()).isEqualTo(secondCreatedAt);
    assertThat(nextCursor.id()).isEqualTo(secondBook.getId());
    assertThat(result.nextAfter()).isEqualTo(secondCreatedAt);
    assertThat(result.size()).isEqualTo(2);
    assertThat(result.totalElements()).isEqualTo(3L);
    assertThat(result.hasNext()).isTrue();

    verify(bookRepository).search(pageRequest);
    verify(bookRepository).count(request);
  }

  @Test
  @DisplayName("도서 목록 조회 결과가 비어있으면 빈 커서 페이지 응답을 반환")
  void searchBooksWithEmptyResult() {
    // given
    BookSearchRequest request =
        BookSearchRequest.of(
            "없는 책",
            BookOrderBy.TITLE,
            Sort.Direction.ASC,
            null,
            10
        );

    BookSearchRequest pageRequest = request.withLimit(request.limit() + 1);

    when(bookRepository.search(pageRequest)).thenReturn(List.of());
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

    verify(bookRepository).search(pageRequest);
    verify(bookRepository).count(request);
  }

  @Test
  @DisplayName("도서 정보를 수정하면 수정된 BookDto 반환")
  void update() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Book book = book()
        .id(bookId)
        .userId(requestUserId)
        .title("수정 전 제목")
        .author("수정 전 작가")
        .description("수정 전 설명")
        .publisher("수정 전 출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9788960177758")
        .thumbnailUrl("https://example.com/before.jpg")
        .build();

    BookUpdateRequest request =
        new BookUpdateRequest(
            "수정 후 제목",
            "수정 후 작가",
            "수정 후 설명",
            "수정 후 출판사",
            LocalDate.of(2026, 5, 28),
            "https://example.com/after.jpg"
        );

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.of(book));

    // when
    BookDto result = bookService.update(bookId, requestUserId, request, null);

    // then
    assertThat(result.id()).isEqualTo(bookId);
    assertThat(result.title()).isEqualTo(request.title());
    assertThat(result.author()).isEqualTo(request.author());
    assertThat(result.description()).isEqualTo(request.description());
    assertThat(result.publisher()).isEqualTo(request.publisher());
    assertThat(result.publishedDate()).isEqualTo(request.publishedDate());
    assertThat(result.isbn()).isEqualTo("9788960177758");
    assertThat(result.thumbnailUrl()).isEqualTo(request.thumbnailUrl());

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
    verify(s3Service, never()).upload(any(MultipartFile.class), any(String.class));
  }

  @Test
  @DisplayName("도서 수정 시 썸네일 이미지가 있으면 S3 업로드 URL을 저장")
  void updateBookWithThumbnailImage() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Book book = book()
        .id(bookId)
        .userId(requestUserId)
        .title("수정 전 제목")
        .author("수정 전 작가")
        .description("수정 전 설명")
        .publisher("수정 전 출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9788960177758")
        .thumbnailUrl("https://example.com/before.jpg")
        .build();

    BookUpdateRequest request =
        new BookUpdateRequest(
            "수정 후 제목",
            "수정 후 작가",
            "수정 후 설명",
            "수정 후 출판사",
            LocalDate.of(2026, 5, 28),
            null
        );

    MultipartFile thumbnailImage =
        new MockMultipartFile(
            "thumbnailImage",
            "updated-thumbnail.jpg",
            "image/jpeg",
            "updated-image".getBytes()
        );

    String uploadedUrl = "https://s3.example.com/books/updated-thumbnail.jpg";

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.of(book));
    when(s3Service.upload(any(MultipartFile.class), any(String.class))).thenReturn(uploadedUrl);

    // when
    BookDto result = bookService.update(bookId, requestUserId, request, thumbnailImage);

    // then
    assertThat(result.id()).isEqualTo(bookId);
    assertThat(result.title()).isEqualTo(request.title());
    assertThat(result.author()).isEqualTo(request.author());
    assertThat(result.description()).isEqualTo(request.description());
    assertThat(result.publisher()).isEqualTo(request.publisher());
    assertThat(result.publishedDate()).isEqualTo(request.publishedDate());
    assertThat(result.isbn()).isEqualTo("9788960177758");
    assertThat(result.thumbnailUrl()).isEqualTo(uploadedUrl);
    assertThat(book.getThumbnailUrl()).isEqualTo(uploadedUrl);

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
    verify(s3Service).upload(any(MultipartFile.class), any(String.class));
  }

  @Test
  @DisplayName("존재하지 않는 도서를 수정하면 예외 발생")
  void updateBookWithNotFound() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    BookUpdateRequest request =
        new BookUpdateRequest(
            "수정 후 제목",
            "수정 후 저자",
            "수정 후 설명",
            "수정 후 출판사",
            LocalDate.of(2026, 5, 28),
            "https://example.com/new.jpg"
        );

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> bookService.update(bookId, requestUserId, request, null))
        .isInstanceOf(BookNotFoundException.class);

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
    verify(s3Service, never()).upload(any(MultipartFile.class), any(String.class));
  }

  @Test
  @DisplayName("등록자가 아닌 사용자가 도서를 수정하면 예외 발생")
  void updateBookWithForbiddenUser() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Book book = book()
        .id(bookId)
        .userId(ownerId)
        .title("수정 전 제목")
        .author("수정 전 작가")
        .description("수정 전 설명")
        .publisher("수정 전 출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9788960177758")
        .thumbnailUrl("https://example.com/before.jpg")
        .build();

    BookUpdateRequest request =
        new BookUpdateRequest(
            "수정 후 제목",
            "수정 후 저자",
            "수정 후 설명",
            "수정 후 출판사",
            LocalDate.of(2026, 5, 28),
            "https://example.com/new.jpg"
        );

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.of(book));

    // when, then
    assertThatThrownBy(() -> bookService.update(bookId, requestUserId, request, null))
        .isInstanceOf(BookForbiddenException.class);

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
    verify(s3Service, never()).upload(any(MultipartFile.class), any(String.class));
  }

  @Test
  @DisplayName("도서를 논리 삭제하면 deletedAt이 설정됨")
  void deleteBook() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Book book = book()
        .id(bookId)
        .userId(requestUserId)
        .title("삭제할 도서")
        .author("삭제할 작가")
        .description("삭제할 설명")
        .publisher("삭제할 출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9780000004301")
        .thumbnailUrl("https://example.com/delete.jpg")
        .build();

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.of(book));

    // when
    bookService.delete(bookId, requestUserId);

    // then
    assertThat(book.isDeleted()).isTrue();
    assertThat(book.getDeletedAt()).isNotNull();

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
  }

  @Test
  @DisplayName("존재하지 않는 도서를 논리 삭제하면 예외 발생")
  void deleteBookWithNotFound() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> bookService.delete(bookId, requestUserId))
        .isInstanceOf(BookNotFoundException.class);

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
  }

  @Test
  @DisplayName("등록자가 아닌 사용자가 도서를 논리 삭제하면 예외 발생")
  void deleteBookWithForbiddenUser() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Book book = book()
        .id(bookId)
        .userId(ownerId)
        .title("삭제할 도서")
        .author("삭제할 작가")
        .description("삭제할 설명")
        .publisher("삭제할 출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9780000004302")
        .thumbnailUrl("https://example.com/delete-forbidden.jpg")
        .build();

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.of(book));

    // when, then
    assertThatThrownBy(() -> bookService.delete(bookId, requestUserId))
        .isInstanceOf(BookForbiddenException.class);

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
  }

  @Test
  @DisplayName("도서를 물리 삭제하면 Repository delete를 호출함")
  void hardDeleteBook() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Book book = book()
        .id(bookId)
        .userId(requestUserId)
        .title("물리 삭제할 도서")
        .author("물리 삭제할 작가")
        .description("물리 삭제할 설명")
        .publisher("물리 삭제할 출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9780000004401")
        .thumbnailUrl("https://example.com/hard-delete.jpg")
        .build();

    when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

    // when
    bookService.hardDelete(bookId, requestUserId);

    // then
    verify(bookRepository).findById(bookId);
    verify(bookRepository).delete(book);
  }

  @Test
  @DisplayName("존재하지 않는 도서를 물리 삭제하면 예외 발생")
  void hardDeleteBookWithNotFound() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    when(bookRepository.findById(bookId)).thenReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> bookService.hardDelete(bookId, requestUserId))
        .isInstanceOf(BookNotFoundException.class);

    verify(bookRepository).findById(bookId);
    verify(bookRepository, never()).delete(any(Book.class));
  }

  @Test
  @DisplayName("등록자가 아닌 사용자가 도서를 물리 삭제하면 예외 발생")
  void hardDeleteBookWithForbiddenUser() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Book book = book()
        .id(bookId)
        .userId(ownerId)
        .title("물리 삭제할 도서")
        .author("물리 삭제할 작가")
        .description("물리 삭제할 설명")
        .publisher("물리 삭제할 출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9780000004402")
        .thumbnailUrl("https://example.com/hard-delete-forbidden.jpg")
        .build();

    when(bookRepository.findById(bookId)).thenReturn(Optional.of(book));

    // when, then
    assertThatThrownBy(() -> bookService.hardDelete(bookId, requestUserId))
        .isInstanceOf(BookForbiddenException.class);

    verify(bookRepository).findById(bookId);
    verify(bookRepository, never()).delete(any(Book.class));
  }

  @Test
  @DisplayName("썸네일 이미지가 비어있으면 S3에 업로드하지 않고 요청 thumbnailUrl을 사용한다")
  void createBookWithEmptyThumbnailImage() {
    // given
    UUID requestUserId = UUID.randomUUID();

    BookCreateRequest request =
        new BookCreateRequest(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            "https://example.com/fallback.jpg"
        );

    MultipartFile emptyThumbnailImage =
        new MockMultipartFile(
            "thumbnailImage",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

    when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    BookDto result = bookService.create(requestUserId, request, emptyThumbnailImage);

    // then
    assertThat(result.thumbnailUrl()).isEqualTo(request.thumbnailUrl());

    ArgumentCaptor<Book> bookCaptor = ArgumentCaptor.forClass(Book.class);
    verify(bookRepository).save(bookCaptor.capture());

    Book savedBook = bookCaptor.getValue();
    assertThat(savedBook.getThumbnailUrl()).isEqualTo(request.thumbnailUrl());

    verify(s3Service, never()).upload(any(MultipartFile.class), any(String.class));
  }

  @Test
  @DisplayName("썸네일 이미지 원본 파일명이 null이어도 S3에 업로드한다")
  void createBookWithNullOriginalFilenameThumbnailImage() {
    // given
    UUID requestUserId = UUID.randomUUID();

    BookCreateRequest request =
        new BookCreateRequest(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            null
        );

    MultipartFile thumbnailImage =
        new MockMultipartFile(
            "thumbnailImage",
            null,
            "image/jpeg",
            "test-image".getBytes()
        );

    String uploadedUrl = "https://s3.example.com/books/thumbnail.jpg";

    when(s3Service.upload(any(MultipartFile.class), any(String.class))).thenReturn(uploadedUrl);
    when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    BookDto result = bookService.create(requestUserId, request, thumbnailImage);

    // then
    assertThat(result.thumbnailUrl()).isEqualTo(uploadedUrl);

    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3Service).upload(any(MultipartFile.class), keyCaptor.capture());

    assertThat(keyCaptor.getValue()).contains("books/");
    assertThat(keyCaptor.getValue()).contains(requestUserId.toString());
    assertThat(keyCaptor.getValue()).contains("thumbnail");
  }

  @Test
  @DisplayName("도서 수정 시 썸네일 이미지가 비어있으면 S3에 업로드하지 않고 요청 thumbnailUrl을 사용한다")
  void updateBookWithEmptyThumbnailImage() {
    // given
    UUID bookId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    Book book = book()
        .id(bookId)
        .userId(requestUserId)
        .title("수정 전 제목")
        .author("수정 전 작가")
        .description("수정 전 설명")
        .publisher("수정 전 출판사")
        .publishedDate(LocalDate.of(2026, 1, 1))
        .isbn("9788960177758")
        .thumbnailUrl("https://example.com/before.jpg")
        .build();

    BookUpdateRequest request =
        new BookUpdateRequest(
            "수정 후 제목",
            "수정 후 작가",
            "수정 후 설명",
            "수정 후 출판사",
            LocalDate.of(2026, 5, 28),
            "https://example.com/fallback-after.jpg"
        );

    MultipartFile emptyThumbnailImage =
        new MockMultipartFile(
            "thumbnailImage",
            "empty.jpg",
            "image/jpeg",
            new byte[0]
        );

    when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.of(book));

    // when
    BookDto result = bookService.update(bookId, requestUserId, request, emptyThumbnailImage);

    // then
    assertThat(result.thumbnailUrl()).isEqualTo(request.thumbnailUrl());
    assertThat(book.getThumbnailUrl()).isEqualTo(request.thumbnailUrl());

    verify(bookRepository).findByIdAndDeletedAtIsNull(bookId);
    verify(s3Service, never()).upload(any(MultipartFile.class), any(String.class));
  }

  @Test
  @DisplayName("도서 생성 중 트랜잭션이 롤백되면 업로드한 S3 객체를 삭제한다")
  void createBookWithThumbnailImageRollbackDeletesUploadedS3Object() {
    // given
    TransactionSynchronizationManager.initSynchronization();

    try {
      UUID requestUserId = UUID.randomUUID();

      BookCreateRequest request =
          new BookCreateRequest(
              "그리고 아무도 없었다",
              "애거서 크리스티",
              "외딴 섬에서 벌어지는 연쇄 살인 사건",
              "황금가지",
              LocalDate.of(2013, 12, 31),
              "9788960177758",
              null
          );

      MultipartFile thumbnailImage =
          new MockMultipartFile(
              "thumbnailImage",
              "thumbnail.jpg",
              "image/jpeg",
              "test-image".getBytes()
          );

      String uploadedUrl = "https://s3.example.com/books/thumbnail.jpg";

      when(s3Service.upload(any(MultipartFile.class), any(String.class))).thenReturn(uploadedUrl);
      when(bookRepository.save(any(Book.class))).thenAnswer(
          invocation -> invocation.getArgument(0));

      // when
      bookService.create(requestUserId, request, thumbnailImage);

      List<TransactionSynchronization> synchronizations =
          TransactionSynchronizationManager.getSynchronizations();

      synchronizations.forEach(
          synchronization ->
              synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
      );

      // then
      ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

      verify(s3Service).upload(any(MultipartFile.class), keyCaptor.capture());
      verify(s3Service).delete(keyCaptor.getValue());

      assertThat(keyCaptor.getValue()).startsWith("books/" + requestUserId + "/");
      assertThat(keyCaptor.getValue()).contains("thumbnail.jpg");
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName("도서 수정 중 트랜잭션이 롤백되면 업로드한 S3 객체를 삭제한다")
  void updateBookWithThumbnailImageRollbackDeletesUploadedS3Object() {
    // given
    TransactionSynchronizationManager.initSynchronization();

    try {
      UUID bookId = UUID.randomUUID();
      UUID requestUserId = UUID.randomUUID();

      Book book = book()
          .id(bookId)
          .userId(requestUserId)
          .title("수정 전 제목")
          .author("수정 전 작가")
          .description("수정 전 설명")
          .publisher("수정 전 출판사")
          .publishedDate(LocalDate.of(2026, 1, 1))
          .isbn("9788960177758")
          .thumbnailUrl("https://example.com/before.jpg")
          .build();

      BookUpdateRequest request =
          new BookUpdateRequest(
              "수정 후 제목",
              "수정 후 작가",
              "수정 후 설명",
              "수정 후 출판사",
              LocalDate.of(2026, 5, 28),
              null
          );

      MultipartFile thumbnailImage =
          new MockMultipartFile(
              "thumbnailImage",
              "updated thumbnail.jpg",
              "image/jpeg",
              "updated-image".getBytes()
          );

      String uploadedUrl = "https://s3.example.com/books/updated-thumbnail.jpg";

      when(bookRepository.findByIdAndDeletedAtIsNull(bookId)).thenReturn(Optional.of(book));
      when(s3Service.upload(any(MultipartFile.class), any(String.class))).thenReturn(uploadedUrl);

      // when
      bookService.update(bookId, requestUserId, request, thumbnailImage);

      List<TransactionSynchronization> synchronizations =
          TransactionSynchronizationManager.getSynchronizations();

      synchronizations.forEach(
          synchronization ->
              synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
      );

      // then
      ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

      verify(s3Service).upload(any(MultipartFile.class), keyCaptor.capture());
      verify(s3Service).delete(keyCaptor.getValue());

      assertThat(keyCaptor.getValue()).startsWith("books/" + requestUserId + "/");
      assertThat(keyCaptor.getValue()).contains("updated_thumbnail.jpg");
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName("썸네일 이미지 원본 파일명의 특수문자는 S3 key에서 안전하게 치환된다")
  void createBookWithUnsafeOriginalFilenameThumbnailImage() {
    // given
    UUID requestUserId = UUID.randomUUID();

    BookCreateRequest request =
        new BookCreateRequest(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            null
        );

    MultipartFile thumbnailImage =
        new MockMultipartFile(
            "thumbnailImage",
            "my thumbnail @2026!.jpg",
            "image/jpeg",
            "test-image".getBytes()
        );

    String uploadedUrl = "https://s3.example.com/books/thumbnail.jpg";

    when(s3Service.upload(any(MultipartFile.class), any(String.class))).thenReturn(uploadedUrl);
    when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    bookService.create(requestUserId, request, thumbnailImage);

    // then
    ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);

    verify(s3Service).upload(any(MultipartFile.class), keyCaptor.capture());

    assertThat(keyCaptor.getValue()).startsWith("books/" + requestUserId + "/");
    assertThat(keyCaptor.getValue()).contains("my_thumbnail__2026_.jpg");
  }

  @Test
  @DisplayName("트랜잭션 동기화가 없으면 S3 롤백 보상 삭제를 등록하지 않는다")
  void createBookWithThumbnailImageWithoutTransactionSynchronizationDoesNotRegisterRollbackCleanup() {
    // given
    UUID requestUserId = UUID.randomUUID();

    BookCreateRequest request =
        new BookCreateRequest(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            null
        );

    MultipartFile thumbnailImage =
        new MockMultipartFile(
            "thumbnailImage",
            "thumbnail.jpg",
            "image/jpeg",
            "test-image".getBytes()
        );

    String uploadedUrl = "https://s3.example.com/books/thumbnail.jpg";

    when(s3Service.upload(any(MultipartFile.class), any(String.class))).thenReturn(uploadedUrl);
    when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

    // when
    BookDto result = bookService.create(requestUserId, request, thumbnailImage);

    // then
    assertThat(result.thumbnailUrl()).isEqualTo(uploadedUrl);

    verify(s3Service).upload(any(MultipartFile.class), any(String.class));
    verify(s3Service, never()).delete(any(String.class));
  }

  @Test
  @DisplayName("도서 생성 트랜잭션이 커밋되면 업로드한 S3 객체를 삭제하지 않는다")
  void createBookWithThumbnailImageCommittedTransactionDoesNotDeleteUploadedS3Object() {
    // given
    TransactionSynchronizationManager.initSynchronization();

    try {
      UUID requestUserId = UUID.randomUUID();

      BookCreateRequest request =
          new BookCreateRequest(
              "그리고 아무도 없었다",
              "애거서 크리스티",
              "외딴 섬에서 벌어지는 연쇄 살인 사건",
              "황금가지",
              LocalDate.of(2013, 12, 31),
              "9788960177758",
              null
          );

      MultipartFile thumbnailImage =
          new MockMultipartFile(
              "thumbnailImage",
              "thumbnail.jpg",
              "image/jpeg",
              "test-image".getBytes()
          );

      String uploadedUrl = "https://s3.example.com/books/thumbnail.jpg";

      when(s3Service.upload(any(MultipartFile.class), any(String.class))).thenReturn(uploadedUrl);
      when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

      // when
      bookService.create(requestUserId, request, thumbnailImage);

      List<TransactionSynchronization> synchronizations =
          TransactionSynchronizationManager.getSynchronizations();

      synchronizations.forEach(
          synchronization ->
              synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED)
      );

      // then
      verify(s3Service).upload(any(MultipartFile.class), any(String.class));
      verify(s3Service, never()).delete(any(String.class));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }

  @Test
  @DisplayName("트랜잭션 롤백 후 S3 보상 삭제가 실패해도 예외가 전파되지 않는다")
  void createBookWithThumbnailImageRollbackCleanupFailureDoesNotThrowException() {
    // given
    TransactionSynchronizationManager.initSynchronization();

    try {
      UUID requestUserId = UUID.randomUUID();

      BookCreateRequest request =
          new BookCreateRequest(
              "그리고 아무도 없었다",
              "애거서 크리스티",
              "외딴 섬에서 벌어지는 연쇄 살인 사건",
              "황금가지",
              LocalDate.of(2013, 12, 31),
              "9788960177758",
              null
          );

      MultipartFile thumbnailImage =
          new MockMultipartFile(
              "thumbnailImage",
              "thumbnail.jpg",
              "image/jpeg",
              "test-image".getBytes()
          );

      String uploadedUrl = "https://s3.example.com/books/thumbnail.jpg";

      when(s3Service.upload(any(MultipartFile.class), any(String.class))).thenReturn(uploadedUrl);
      when(bookRepository.save(any(Book.class))).thenAnswer(invocation -> invocation.getArgument(0));

      doThrow(new RuntimeException("S3 delete failed"))
          .when(s3Service)
          .delete(any(String.class));

      // when
      bookService.create(requestUserId, request, thumbnailImage);

      List<TransactionSynchronization> synchronizations =
          TransactionSynchronizationManager.getSynchronizations();

      // then
      assertThatCode(() ->
          synchronizations.forEach(
              synchronization ->
                  synchronization.afterCompletion(TransactionSynchronization.STATUS_ROLLED_BACK)
          )
      ).doesNotThrowAnyException();

      verify(s3Service).upload(any(MultipartFile.class), any(String.class));
      verify(s3Service).delete(any(String.class));
    } finally {
      TransactionSynchronizationManager.clearSynchronization();
    }
  }
}