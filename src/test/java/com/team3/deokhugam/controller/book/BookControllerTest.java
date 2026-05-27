package com.team3.deokhugam.controller.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.exception.book.BookAlreadyExistsException;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.service.book.BookService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
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
}
