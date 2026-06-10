package com.team3.deokhugam.client.ocr;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.team3.deokhugam.client.ocr.dto.OcrResultDto;
import com.team3.deokhugam.exception.ocr.OcrApiException;
import com.team3.deokhugam.global.config.OcrProperties;
import com.team3.deokhugam.service.image.ImageOptimizer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class OcrClientTest {

  private static final String OCR_API_URL = "https://api.ocr.space/parse/image";
  private static final String OCR_API_KEY = "ocr-api-key";

  private RestTemplate restTemplate;
  private MockRestServiceServer server;
  private ImageOptimizer imageOptimizer;
  private OcrClient ocrClient;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    server = MockRestServiceServer.createServer(restTemplate);
    imageOptimizer = new ImageOptimizer();

    ocrClient =
        new OcrClient(
            restTemplate,
            new OcrProperties(OCR_API_KEY, OCR_API_URL, 15000, 1),
            imageOptimizer
        );
  }

  @Test
  @DisplayName("이미지를 OCR Space API로 전송하고 응답을 반환한다")
  void parseImage() {
    // given
    MockMultipartFile file = new MockMultipartFile(
        "image",
        "book.jpg",
        MediaType.IMAGE_JPEG_VALUE,
        "test-image".getBytes()
    );

    String responseJson = """
        {
          "ParsedResults": [
            {
              "FileParseExitCode": 1,
              "ParsedText": "ISBN 978-89-6540-260-2",
              "ErrorMessage": "",
              "ErrorDetails": ""
            }
          ],
          "OCRExitCode": 1,
          "IsErroredOnProcessing": false,
          "ErrorMessage": null,
          "ErrorDetails": null,
          "ProcessingTimeInMilliseconds": "624"
        }
        """;

    server.expect(once(), requestTo(OCR_API_URL))
        .andExpect(method(HttpMethod.POST))
        .andExpect(header("apikey", OCR_API_KEY))
        .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

    // when
    OcrResultDto result = ocrClient.parseImage(file);

    // then
    assertThat(result).isNotNull();
    assertThat(result.hasProcessingError()).isFalse();
    assertThat(result.mergedParsedText()).contains("ISBN 978-89-6540-260-2");

    server.verify();
  }

  @Test
  @DisplayName("OCR API 설정값이 비어 있으면 예외가 발생한다")
  void parseImageWithMissingProperties() {
    // given
    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "book.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test-image".getBytes()
        );

    OcrClient client =
        new OcrClient(
            restTemplate,
            new OcrProperties("", OCR_API_URL, 15000, 1),
            imageOptimizer
        );

    // when, then
    assertThatThrownBy(() -> client.parseImage(image))
        .isInstanceOf(OcrApiException.class);
  }

  @Test
  @DisplayName("OCR API 호출이 실패하면 예외가 발생한다")
  void parseImageWithApiFailure() {
    // given
    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "book.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test-image".getBytes()
        );

    server.expect(once(), requestTo(OCR_API_URL))
        .andExpect(method(HttpMethod.POST))
        .andRespond(withServerError());

    // when, then
    assertThatThrownBy(() -> ocrClient.parseImage(image))
        .isInstanceOf(OcrApiException.class);

    server.verify();
  }
}
