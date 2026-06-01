package com.team3.deokhugam.controller.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookCursor;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.dto.book.BookOrderBy;
import com.team3.deokhugam.dto.book.BookSearchRequest;
import com.team3.deokhugam.dto.book.BookUpdateRequest;
import com.team3.deokhugam.exception.book.BookAlreadyExistsException;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.book.BookService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookController.class)
class BookControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private BookService bookService;

  @Test
  @DisplayName("도서 생성")
  void createBook() throws Exception {
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

    Instant now = Instant.parse("2026-05-26T02:03:32.227Z");

    BookDto response =
        new BookDto(
            UUID.randomUUID(),
            request.title(),
            request.author(),
            request.description(),
            request.publisher(),
            request.publishedDate(),
            request.isbn(),
            request.thumbnailUrl(),
            0,
            BigDecimal.ZERO,
            now,
            now
        );

    when(bookService.create(any(BookCreateRequest.class))).thenReturn(response);

    // when, then
    MockMultipartFile bookData =
        new MockMultipartFile("bookData", "bookData.json",
            MediaType.APPLICATION_JSON_VALUE, objectMapper.writeValueAsBytes(request));

    mockMvc.perform(
            multipart("/api/books").file(bookData))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.title").value(request.title()))
        .andExpect(jsonPath("$.author").value(request.author()))
        .andExpect(jsonPath("$.isbn").value(request.isbn()));
  }

  @Test
  @DisplayName("필수값이 누락되면 도서를 생성할 수 없다")
  void createBookWithoutRequiredField() throws Exception {
    // given
    BookCreateRequest request =
        new BookCreateRequest(
            "",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            "https://example.com/book.jpg");

    MockMultipartFile bookData =
        new MockMultipartFile(
            "bookData",
            "bookData.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request));

    // when, then
    mockMvc
        .perform(multipart("/api/books").file(bookData))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("ISBN이 중복되면 409를 반환한다")
  void createBookWithDuplicateIsbn() throws Exception {
    // given
    BookCreateRequest request =
        new BookCreateRequest(
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            "https://example.com/book.jpg");

    MockMultipartFile bookData =
        new MockMultipartFile(
            "bookData",
            "bookData.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request));

    when(bookService.create(any(BookCreateRequest.class)))
        .thenThrow(new BookAlreadyExistsException());

    // when, then
    mockMvc
        .perform(multipart("/api/books").file(bookData))
        .andExpect(status().isConflict());
  }

  @Test
  @DisplayName("도서 ID로 상세 조회하면 200 응답과 BookDto를 반환")
  void findBookById() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();
    Instant now = Instant.parse("2026-05-26T02:03:32.227Z");

    BookDto response =
        new BookDto(
            bookId,
            "그리고 아무도 없었다",
            "애거서 크리스티",
            "외딴 섬에서 벌어지는 연쇄 살인 사건",
            "황금가지",
            LocalDate.of(2013, 12, 31),
            "9788960177758",
            "https://example.com/book.jpg",
            0,
            BigDecimal.ZERO,
            now,
            now
        );

    when(bookService.findById(bookId)).thenReturn(response);

    // when, then
    mockMvc.perform(get("/api/books/{bookId}", bookId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(bookId.toString()))
        .andExpect(jsonPath("$.title").value(response.title()))
        .andExpect(jsonPath("$.author").value(response.author()))
        .andExpect(jsonPath("$.isbn").value(response.isbn()));
  }

  @Test
  @DisplayName("존재하지 않는 도서 ID로 상세 조회하면 404를 반환")
  void findBookByIdWithNotFoundBook() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    when(bookService.findById(bookId)).thenThrow(new BookNotFoundException());

    // when, then
    mockMvc.perform(get("/api/books/{bookId}", bookId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  @DisplayName("도서 목록 조회하면 200 응답과 커서 페이지 응답을 반환")
  void searchBooks() throws Exception {
    // given
    Instant firstCreatedAt = Instant.parse("2026-05-27T00:00:00Z");
    Instant secondCreatedAt = Instant.parse("2026-05-27T00:01:00Z");

    BookDto firstBook =
        new BookDto(
            UUID.randomUUID(),
            "스프링 백엔드",
            "강우진",
            "스프링 백엔드",
            "테스트출판사",
            LocalDate.of(2026, 1, 1),
            "9780000002101",
            "https://example.com/spring-backend.jpg",
            0,
            BigDecimal.ZERO,
            firstCreatedAt,
            secondCreatedAt
        );

    BookDto secondBook =
        new BookDto(
            UUID.randomUUID(),
            "스프링 백엔드2",
            "강우진2",
            "스프링 백엔드2",
            "테스트출판사",
            LocalDate.of(2026, 1, 2),
            "9780000002102",
            "https://example.com/spring-backend2.jpg",
            0,
            BigDecimal.ZERO,
            firstCreatedAt,
            secondCreatedAt
        );

    String nextCursor = BookCursor.encode(
        secondBook.title(),
        secondCreatedAt,
        secondBook.id()
    );

    CursorPageResponse<BookDto> response =
        new CursorPageResponse<>(
            List.of(firstBook, secondBook),
            nextCursor,
            secondCreatedAt,
            2,
            3L,
            true
        );

    when(bookService.search(any())).thenReturn(response);

    // when, then
    mockMvc.perform(
            get("/api/books")
                .param("keyword", "스프링")
                .param("orderBy", "title")
                .param("direction", "ASC")
                .param("limit", "2")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].title").value("스프링 백엔드"))
        .andExpect(jsonPath("$.content[1].title").value("스프링 백엔드2"))
        .andExpect(jsonPath("$.nextCursor").value(nextCursor))
        .andExpect(jsonPath("$.nextAfter").exists())
        .andExpect(jsonPath("$.size").value(2))
        .andExpect(jsonPath("$.totalElements").value(3))
        .andExpect(jsonPath("$.hasNext").value(true));
  }

  @Test
  @DisplayName("잘못된 orderBy로 도서 목록 조회하면 400 응답 반환")
  void searchBooksWithInvalidOrderBy() throws Exception {
    // when, then
    mockMvc.perform(
            get("/api/books")
                .param("orderBy", "wrong")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("잘못된 direction으로 도서 목록 조회하면 400 응답 반환")
  void searchBooksWithInvalidDirection() throws Exception {
    // when, then
    mockMvc.perform(
            get("/api/books")
                .param("direction", "WRONG")
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
        .andExpect(jsonPath("$.status").value(400));
  }

  @Test
  @DisplayName("cursor token으로 도서 목록을 조회한다")
  void searchBooksWithCursorToken() throws Exception {
    // given
    Instant after = Instant.parse("2024-01-01T00:00:00Z");
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

    String cursor = BookCursor.encode("자바", after, id);

    CursorPageResponse<BookDto> response = new CursorPageResponse<>(
        List.of(),
        null,
        null,
        0,
        0L,
        false
    );

    when(bookService.search(any())).thenReturn(response);

    // when, then
    mockMvc.perform(
            get("/api/books")
                .param("cursor", cursor)
                .param("orderBy", "title")
                .param("direction", "ASC")
                .param("limit", "2")
        )
        .andExpect(status().isOk());

    ArgumentCaptor<BookSearchRequest> captor =
        ArgumentCaptor.forClass(BookSearchRequest.class);

    verify(bookService).search(captor.capture());

    BookSearchRequest request = captor.getValue();

    assertThat(request.cursor().value()).isEqualTo("자바");
    assertThat(request.cursor().createdAt()).isEqualTo(after);
    assertThat(request.cursor().id()).isEqualTo(id);
    assertThat(request.limit()).isEqualTo(2);
    assertThat(request.orderBy()).isEqualTo(BookOrderBy.TITLE);
    assertThat(request.direction()).isEqualTo(Sort.Direction.ASC);
  }

  @Test
  @DisplayName("도서 수정하면 200 응답과 수정된 BookDto 반환")
  void updateBook() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();
    Instant now = Instant.parse("2026-05-28T00:00:00Z");

    BookUpdateRequest request =
        new BookUpdateRequest(
            "수정 후 제목",
            "수정 후 작가",
            "수정 후 설명",
            "수정 후 출판사",
            LocalDate.of(2026, 5, 28),
            "https://example.com/after.jpg"
        );

    BookDto response =
        new BookDto(
            bookId,
            request.title(),
            request.author(),
            request.description(),
            request.publisher(),
            request.publishedDate(),
            "9788960177758",
            request.thumbnailUrl(),
            0,
            BigDecimal.ZERO,
            now,
            now
        );

    MockMultipartFile bookData =
        new MockMultipartFile(
            "bookData",
            "bookData.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

    MockMultipartFile thumbnailImage =
        new MockMultipartFile(
            "thumbnailImage",
            "thumbnail.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "thumbnail-image".getBytes()
        );

    when(bookService.update(eq(bookId), any(BookUpdateRequest.class)))
        .thenReturn(response);

    // when, then
    mockMvc.perform(
            multipart("/api/books/{bookId}", bookId)
                .file(bookData)
                .file(thumbnailImage)
                .with(requestBuilder -> {
                  requestBuilder.setMethod("PATCH");
                  return requestBuilder;
                })
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(bookId.toString()))
        .andExpect(jsonPath("$.title").value(request.title()))
        .andExpect(jsonPath("$.author").value(request.author()))
        .andExpect(jsonPath("$.description").value(request.description()))
        .andExpect(jsonPath("$.publisher").value(request.publisher()))
        .andExpect(jsonPath("$.publishedDate").value(request.publishedDate().toString()))
        .andExpect(jsonPath("$.isbn").value("9788960177758"))
        .andExpect(jsonPath("$.thumbnailUrl").value(request.thumbnailUrl()));
  }

  @Test
  @DisplayName("도서 수정은 thumbnamilImage 없이도 가능함")
  void updateBookWithoutTumbnailImage() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();
    Instant now = Instant.parse("2026-05-28T00:00:00Z");

    BookUpdateRequest request =
        new BookUpdateRequest(
            "수정 후 제목",
            "수정 후 작가",
            "수정 후 설명",
            "수정 후 출파사",
            LocalDate.of(2026, 5, 28),
            "https://example.com/after.jpg"
        );

    BookDto response =
        new BookDto(
            bookId,
            request.title(),
            request.author(),
            request.description(),
            request.publisher(),
            request.publishedDate(),
            "9788960177758",
            request.thumbnailUrl(),
            0,
            BigDecimal.ZERO,
            now,
            now
        );

    MockMultipartFile bookData =
        new MockMultipartFile(
            "bookData",
            "bookData.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

    when(bookService.update(eq(bookId), any(BookUpdateRequest.class)))
        .thenReturn(response);

    // when, then
    mockMvc.perform(
            multipart("/api/books/{bookId}", bookId)
                .file(bookData)
                .with(requestBuilder -> {
                  requestBuilder.setMethod("PATCH");
                  return requestBuilder;
                })
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(bookId.toString()))
        .andExpect(jsonPath("$.title").value(request.title()))
        .andExpect(jsonPath("$.author").value(request.author()))
        .andExpect(jsonPath("$.isbn").value("9788960177758"))
        .andExpect(jsonPath("$.thumbnailUrl").value(request.thumbnailUrl()));
  }

  @Test
  @DisplayName("존재하지 않는 도서를 수정하면 404를 반환")
  void updateBookWithNotFoundBook() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    BookUpdateRequest request =
        new BookUpdateRequest(
            "수정 후 제목",
            "수정 후 저자",
            "수정 후 설명",
            "수정 후 출판사",
            LocalDate.of(2026, 5, 28),
            "https://example.com/after.jpg"
        );

    MockMultipartFile bookData =
        new MockMultipartFile(
            "bookData",
            "bookData.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

    when(bookService.update(eq(bookId), any(BookUpdateRequest.class)))
        .thenThrow(new BookNotFoundException());

    // when, then
    mockMvc.perform(
            multipart("/api/books/{bookId}", bookId)
                .file(bookData)
                .with(requestBuilder -> {
                  requestBuilder.setMethod("PATCH");
                  return requestBuilder;
                })
        )
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  @DisplayName("도서를 논리 삭제하면 204를 반환")
  void deleteBook() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    // when, then
    mockMvc.perform(delete("/api/books/{bookId}", bookId))
        .andExpect(status().isNoContent());

    verify(bookService).delete(bookId);
  }

  @Test
  @DisplayName("존재하지 않는 도서를 논리 삭제하면 404를 반환")
  void deleteBookWithNotFoundBook() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    doThrow(new BookNotFoundException()).when(bookService).delete(bookId);

    // when, then
    mockMvc.perform(delete("/api/books/{bookId}", bookId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));

    verify(bookService).delete(bookId);
  }

  @Test
  @DisplayName("도서를 물리 삭제하면 204를 반환")
  void hardDeleteBook() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    // when, then
    mockMvc.perform(delete("/api/books/{bookId}/hard", bookId))
        .andExpect(status().isNoContent());

    verify(bookService).hardDelete(bookId);
  }

  @Test
  @DisplayName("존재하지 않는 도서를 물리 삭제하면 404를 반환")
  void hardDeleteBookWithNotFoundBook() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    doThrow(new BookNotFoundException())
        .when(bookService).hardDelete(bookId);

    // when, then
    mockMvc.perform(delete("/api/books/{bookId}/hard", bookId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BOOK_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));

    verify(bookService).hardDelete(bookId);
  }

  @Test
  @DisplayName("도서 수정 시 출판일이 null이면 400 반환")
  void updateBookWithNullPublishedDate() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    BookUpdateRequest request =
        new BookUpdateRequest(
            "수정 후 제목",
            "수정 후 저자",
            "수정 후 설명",
            "수정 후 출판사",
            null,
            "https://example.com/after.jpg"
        );

    MockMultipartFile bookData =
        new MockMultipartFile(
            "bookData",
            "bookData.json",
            MediaType.APPLICATION_JSON_VALUE,
            objectMapper.writeValueAsBytes(request)
        );

    // when, then
    mockMvc.perform(
            multipart("/api/books/{bookId}", bookId)
                .file(bookData)
                .with(requestBuilder -> {
                  requestBuilder.setMethod("PATCH");
                  return requestBuilder;
                })
        )
        .andExpect(status().isBadRequest());
  }
}