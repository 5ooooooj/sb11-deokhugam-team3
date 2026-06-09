package com.team3.deokhugam.controller.book;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.deokhugam.dto.book.BookInfoDto;
import com.team3.deokhugam.exception.book.BookInfoNotFoundException;
import com.team3.deokhugam.exception.book.InvalidBookIsbnException;
import com.team3.deokhugam.exception.ocr.InvalidOcrImageException;
import com.team3.deokhugam.exception.ocr.OcrIsbnNotFoundException;
import com.team3.deokhugam.service.book.BookService;
import com.team3.deokhugam.service.dashboard.PopularBookService;

import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.multipart.MultipartFile;

@WebMvcTest(BookController.class)
@Import(BookExternalApiControllerTest.MockControllerDependencyConfig.class)
class BookExternalApiControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private BookService bookService;

  @Autowired
  private PopularBookService popularBookService;

  @BeforeEach
  void setUp() {
    reset(bookService, popularBookService);
  }

  @Test
  @DisplayName("ISBN으로 도서 정보를 조회한다")
  void findBookInfoByIsbn() throws Exception {
    // given
    String isbn = "9788965402602";

    BookInfoDto response =
        new BookInfoDto(
            "스프링 부트와 AWS로 혼자 구현하는 웹 서비스",
            "이동욱",
            "스프링 부트 실습서",
            "프리렉",
            LocalDate.of(2019, 11, 29),
            isbn,
            "https://example.com/book.jpg"
        );

    when(bookService.findBookInfoByIsbn(isbn)).thenReturn(response);

    // when, then
    mockMvc.perform(get("/api/books/info").param("isbn", isbn))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.title").value(response.title()))
        .andExpect(jsonPath("$.author").value(response.author()))
        .andExpect(jsonPath("$.description").value(response.description()))
        .andExpect(jsonPath("$.publisher").value(response.publisher()))
        .andExpect(jsonPath("$.publishedDate").value("2019-11-29"))
        .andExpect(jsonPath("$.isbn").value(isbn))
        .andExpect(jsonPath("$.thumbnailImage").value(response.thumbnailImage()));

    verify(bookService).findBookInfoByIsbn(isbn);
  }

  @Test
  @DisplayName("잘못된 ISBN으로 도서 정보 조회 시 400을 반환한다")
  void findBookInfoByInvalidIsbn() throws Exception {
    // given
    String isbn = "invalid-isbn";

    doThrow(new InvalidBookIsbnException())
        .when(bookService)
        .findBookInfoByIsbn(isbn);

    // when, then
    mockMvc.perform(get("/api/books/info").param("isbn", isbn))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_BOOK_ISBN"))
        .andExpect(jsonPath("$.status").value(400));

    verify(bookService).findBookInfoByIsbn(isbn);
  }

  @Test
  @DisplayName("네이버 도서 정보가 없으면 404를 반환한다")
  void findBookInfoByIsbnWithNotFound() throws Exception {
    // given
    String isbn = "8938201007";

    doThrow(new BookInfoNotFoundException())
        .when(bookService)
        .findBookInfoByIsbn(isbn);

    // when, then
    mockMvc.perform(get("/api/books/info").param("isbn", isbn))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("BOOK_INFO_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));

    verify(bookService).findBookInfoByIsbn(isbn);
  }

  @Test
  @DisplayName("OCR 기반 ISBN 인식 성공")
  void recognizeIsbn() throws Exception {
    // given
    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "book.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test-image".getBytes()
        );

    when(bookService.recognizeIsbn(any(MultipartFile.class))).thenReturn("9788965402602");

    // when, then
    mockMvc.perform(multipart("/api/books/isbn/ocr").file(image))
        .andExpect(status().isOk())
        .andExpect(content().string("9788965402602"));

    verify(bookService).recognizeIsbn(any(MultipartFile.class));
  }

  @Test
  @DisplayName("OCR 요청 이미지가 잘못되면 400을 반환한다")
  void recognizeIsbnWithInvalidImage() throws Exception {
    // given
    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "book.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "not-image".getBytes()
        );

    doThrow(new InvalidOcrImageException())
        .when(bookService)
        .recognizeIsbn(any(MultipartFile.class));

    // when, then
    mockMvc.perform(multipart("/api/books/isbn/ocr").file(image))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_OCR_IMAGE"))
        .andExpect(jsonPath("$.status").value(400));

    verify(bookService).recognizeIsbn(any(MultipartFile.class));
  }

  @Test
  @DisplayName("OCR 결과에서 ISBN을 찾지 못하면 400을 반환한다")
  void recognizeIsbnWithIsbnNotFound() throws Exception {
    // given
    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "book.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test-image".getBytes()
        );

    doThrow(new OcrIsbnNotFoundException())
        .when(bookService)
        .recognizeIsbn(any(MultipartFile.class));

    // when, then
    mockMvc.perform(multipart("/api/books/isbn/ocr").file(image))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("OCR_ISBN_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(400));

    verify(bookService).recognizeIsbn(any(MultipartFile.class));
  }

  @TestConfiguration
  static class MockControllerDependencyConfig {

    @Bean
    BookService bookService() {
      return mock(BookService.class);
    }

    @Bean
    PopularBookService popularBookService() {
      return mock(PopularBookService.class);
    }
  }
}