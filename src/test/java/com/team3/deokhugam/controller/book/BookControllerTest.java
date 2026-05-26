package com.team3.deokhugam.controller.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.dto.book.BookCreateRequest;
import com.team3.deokhugam.dto.book.BookDto;
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
}
