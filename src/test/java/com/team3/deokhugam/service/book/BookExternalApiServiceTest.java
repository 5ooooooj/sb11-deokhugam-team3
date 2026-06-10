package com.team3.deokhugam.service.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.client.naver.NaverBookClient;
import com.team3.deokhugam.client.naver.dto.NaverBookItemDto;
import com.team3.deokhugam.client.naver.dto.NaverBookSearchDto;
import com.team3.deokhugam.client.ocr.OcrClient;
import com.team3.deokhugam.client.ocr.dto.OcrParseResultDto;
import com.team3.deokhugam.client.ocr.dto.OcrResultDto;
import com.team3.deokhugam.dto.book.BookInfoDto;
import com.team3.deokhugam.exception.book.BookInfoNotFoundException;
import com.team3.deokhugam.exception.book.InvalidBookIsbnException;
import com.team3.deokhugam.exception.ocr.InvalidOcrImageException;
import com.team3.deokhugam.exception.ocr.OcrIsbnNotFoundException;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.service.s3.S3Service;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
class BookExternalApiServiceTest {

  @Mock
  private BookRepository bookRepository;

  @Mock
  private S3Service s3Service;

  @Mock
  private NaverBookClient naverBookClient;

  @Mock
  private OcrClient ocrClient;

  @InjectMocks
  private BookService bookService;

  @Test
  @DisplayName("ISBN으로 네이버 도서 정보를 조회한다")
  void findBookInfoByIsbn() {
    // given
    String isbn = "978-89-6540-260-2";
    String normalizedIsbn = "9788965402602";

    NaverBookSearchDto response =
        naverResponse(
            naverItem(
                "<b>스프링 부트</b>와 AWS",
                "<b>이동욱</b>",
                "프리렉",
                "8965402609 9788965402602",
                "스프링 &amp; AWS 실습서",
                "20191129"
            )
        );

    when(naverBookClient.searchByIsbn(normalizedIsbn)).thenReturn(response);

    when(naverBookClient.downloadImageAsBase64("https://example.com/book.jpg"))
        .thenReturn("base64-thumbnail");

    // when
    BookInfoDto result = bookService.findBookInfoByIsbn(isbn);

    // then
    assertThat(result.title()).isEqualTo("스프링 부트와 AWS");
    assertThat(result.author()).isEqualTo("이동욱");
    assertThat(result.publisher()).isEqualTo("프리렉");
    assertThat(result.description()).isEqualTo("스프링 & AWS 실습서");
    assertThat(result.publishedDate()).isEqualTo(LocalDate.of(2019, 11, 29));
    assertThat(result.isbn()).isEqualTo(normalizedIsbn);
    assertThat(result.thumbnailImage()).isEqualTo("base64-thumbnail");

    verify(naverBookClient).searchByIsbn(normalizedIsbn);
    verify(naverBookClient).downloadImageAsBase64("https://example.com/book.jpg");
  }

  @Test
  @DisplayName("잘못된 ISBN 형식이면 네이버 API를 호출하지 않고 예외가 발생한다")
  void findBookInfoByInvalidIsbn() {
    // when, then
    assertThatThrownBy(() -> bookService.findBookInfoByIsbn("invalid-isbn"))
        .isInstanceOf(InvalidBookIsbnException.class);

    verify(naverBookClient, never()).searchByIsbn(any(String.class));
  }

  @Test
  @DisplayName("네이버 도서 검색 결과가 없으면 예외가 발생한다")
  void findBookInfoByIsbnWithNoItems() {
    // given
    String isbn = "9788965402602";

    NaverBookSearchDto response =
        new NaverBookSearchDto(
            "Tue, 09 Jun 2026 00:00:00 +0900",
            0,
            1,
            0,
            List.of()
        );

    when(naverBookClient.searchByIsbn(isbn)).thenReturn(response);

    // when, then
    assertThatThrownBy(() -> bookService.findBookInfoByIsbn(isbn))
        .isInstanceOf(BookInfoNotFoundException.class);

    verify(naverBookClient).searchByIsbn(isbn);
  }

  @Test
  @DisplayName("네이버 응답 ISBN이 요청 ISBN과 일치하지 않으면 예외가 발생한다")
  void findBookInfoByIsbnWithMismatchedIsbn() {
    // given
    String requestedIsbn = "9788965402602";

    NaverBookSearchDto response =
        naverResponse(
            naverItem(
                "다른 책",
                "다른 저자",
                "다른 출판사",
                "1111111111 9781111111111",
                "다른 설명",
                "20200101"
            )
        );

    when(naverBookClient.searchByIsbn(requestedIsbn)).thenReturn(response);

    // 방어 확인:
    // 테스트 데이터가 진짜로 요청 ISBN을 포함하지 않는지 먼저 검증합니다.
    assertThat(response.items().get(0).isbn()).doesNotContain(requestedIsbn);

    // when, then
    assertThatThrownBy(() -> bookService.findBookInfoByIsbn(requestedIsbn))
        .isInstanceOf(BookInfoNotFoundException.class);

    verify(naverBookClient).searchByIsbn(requestedIsbn);
  }

  @Test
  @DisplayName("네이버 pubdate 파싱에 실패하면 publishedDate는 null이다")
  void findBookInfoByIsbnWithInvalidPublishedDate() {
    // given
    String isbn = "9788965402602";

    NaverBookSearchDto response =
        naverResponse(
            naverItem(
                "스프링 부트와 AWS",
                "이동욱",
                "프리렉",
                "8965402609 9788965402602",
                "스프링 실습서",
                "invalid-date"
            )
        );

    when(naverBookClient.searchByIsbn(isbn)).thenReturn(response);

    // when
    BookInfoDto result = bookService.findBookInfoByIsbn(isbn);

    // then
    assertThat(result.publishedDate()).isNull();
    assertThat(result.isbn()).isEqualTo(isbn);

    verify(naverBookClient).searchByIsbn(isbn);
  }

  @Test
  @DisplayName("OCR 결과에서 ISBN-13을 추출한다")
  void recognizeIsbnWithIsbn13() {
    // given
    MockMultipartFile image = imageFile();

    OcrResultDto ocrResult =
        new OcrResultDto(
            List.of(
                new OcrParseResultDto(
                    1,
                    "도서 뒷면 텍스트\nISBN 978-89-6540-260-2",
                    "",
                    ""
                )
            ),
            1,
            false,
            null,
            null,
            "624"
        );

    when(ocrClient.parseImage(image)).thenReturn(ocrResult);

    // when
    String result = bookService.recognizeIsbn(image);

    // then
    assertThat(result).isEqualTo("9788965402602");
    verify(ocrClient).parseImage(image);
  }

  @Test
  @DisplayName("OCR 결과에서 ISBN-10을 추출한다")
  void recognizeIsbnWithIsbn10() {
    // given
    MockMultipartFile image = imageFile();

    OcrResultDto ocrResult =
        new OcrResultDto(
            List.of(
                new OcrParseResultDto(
                    1,
                    "AGATHA CHRISTIE\nISBN 89-382-0100-7",
                    "",
                    ""
                )
            ),
            1,
            false,
            null,
            null,
            "624"
        );

    when(ocrClient.parseImage(image)).thenReturn(ocrResult);

    // when
    String result = bookService.recognizeIsbn(image);

    // then
    assertThat(result).isEqualTo("8938201007");
    verify(ocrClient).parseImage(image);
  }

  @Test
  @DisplayName("OCR 이미지가 비어 있으면 예외가 발생한다")
  void recognizeIsbnWithEmptyImage() {
    // given
    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "book.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            new byte[0]
        );

    // when, then
    assertThatThrownBy(() -> bookService.recognizeIsbn(image))
        .isInstanceOf(InvalidOcrImageException.class);

    verify(ocrClient, never()).parseImage(any(MultipartFile.class));
  }

  @Test
  @DisplayName("OCR 파일이 이미지가 아니면 예외가 발생한다")
  void recognizeIsbnWithNonImageFile() {
    // given
    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "book.txt",
            MediaType.TEXT_PLAIN_VALUE,
            "ISBN 9788965402602".getBytes()
        );

    // when, then
    assertThatThrownBy(() -> bookService.recognizeIsbn(image))
        .isInstanceOf(InvalidOcrImageException.class);

    verify(ocrClient, never()).parseImage(any(MultipartFile.class));
  }

  @Test
  @DisplayName("OCR API 처리 실패 응답이면 예외가 발생한다")
  void recognizeIsbnWithProcessingError() {
    // given
    MockMultipartFile image = imageFile();

    OcrResultDto ocrResult =
        new OcrResultDto(
            List.of(),
            3,
            true,
            "OCR processing failed",
            "Invalid image",
            "624"
        );

    when(ocrClient.parseImage(image)).thenReturn(ocrResult);

    // when, then
    assertThatThrownBy(() -> bookService.recognizeIsbn(image))
        .isInstanceOf(OcrIsbnNotFoundException.class);

    verify(ocrClient).parseImage(image);
  }

  @Test
  @DisplayName("OCR 결과에서 ISBN을 찾지 못하면 예외가 발생한다")
  void recognizeIsbnWithoutIsbn() {
    // given
    MockMultipartFile image = imageFile();

    OcrResultDto ocrResult =
        new OcrResultDto(
            List.of(
                new OcrParseResultDto(
                    1,
                    "도서 제목과 저자만 인식되었습니다.",
                    "",
                    ""
                )
            ),
            1,
            false,
            null,
            null,
            "624"
        );

    when(ocrClient.parseImage(image)).thenReturn(ocrResult);

    // when, then
    assertThatThrownBy(() -> bookService.recognizeIsbn(image))
        .isInstanceOf(OcrIsbnNotFoundException.class);

    verify(ocrClient).parseImage(image);
  }

  private NaverBookSearchDto naverResponse(NaverBookItemDto item) {
    return new NaverBookSearchDto(
        "Tue, 09 Jun 2026 00:00:00 +0900",
        1,
        1,
        1,
        List.of(item)
    );
  }

  private NaverBookItemDto naverItem(
      String title,
      String author,
      String publisher,
      String isbn,
      String description,
      String pubdate
  ) {
    return new NaverBookItemDto(
        title,
        "https://example.com/book",
        "https://example.com/book.jpg",
        author,
        "19800",
        publisher,
        isbn,
        description,
        pubdate
    );
  }

  private MockMultipartFile imageFile() {
    return new MockMultipartFile(
        "image",
        "book.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "test-image".getBytes()
    );
  }

  @Test
  @DisplayName("네이버 author 값의 ^ 구분자를 쉼표로 변환한다")
  void findBookInfoByIsbnWithMultipleAuthors() {
    // given
    String isbn = "9788965402602";

    NaverBookSearchDto response =
        naverResponse(
            naverItem(
                "테스트 도서",
                "윤영빈^서용욱^박인상^정상온",
                "테스트 출판사",
                "8965402609 9788965402602",
                "테스트 설명",
                "20260101"
            )
        );

    when(naverBookClient.searchByIsbn(isbn)).thenReturn(response);
    when(naverBookClient.downloadImageAsBase64("https://example.com/book.jpg"))
        .thenReturn("base64-thumbnail");

    // when
    BookInfoDto result = bookService.findBookInfoByIsbn(isbn);

    // then
    assertThat(result.author()).isEqualTo("윤영빈, 서용욱, 박인상, 정상온");
    assertThat(result.thumbnailImage()).isEqualTo("base64-thumbnail");
  }

  @Test
  @DisplayName("네이버 description이 1000자를 초과하면 말줄임표를 붙여 1000자 이하로 응답한다")
  void findBookInfoByIsbnWithLongDescription() {
    // given
    String isbn = "9788965402602";
    String longDescription = "가".repeat(1001);

    NaverBookSearchDto response =
        naverResponse(
            naverItem(
                "테스트 도서",
                "테스트 저자",
                "테스트 출판사",
                "8965402609 9788965402602",
                longDescription,
                "20260101"
            )
        );

    when(naverBookClient.searchByIsbn(isbn)).thenReturn(response);
    when(naverBookClient.downloadImageAsBase64("https://example.com/book.jpg"))
        .thenReturn("base64-thumbnail");

    // when
    BookInfoDto result = bookService.findBookInfoByIsbn(isbn);

    // then
    assertThat(result.description()).hasSize(1000);
    assertThat(result.description()).endsWith("...");
    assertThat(result.thumbnailImage()).isEqualTo("base64-thumbnail");

    verify(naverBookClient).searchByIsbn(isbn);
    verify(naverBookClient).downloadImageAsBase64("https://example.com/book.jpg");
  }
}