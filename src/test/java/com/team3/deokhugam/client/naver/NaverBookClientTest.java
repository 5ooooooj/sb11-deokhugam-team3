package com.team3.deokhugam.client.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.client.naver.dto.NaverBookSearchDto;
import com.team3.deokhugam.exception.naver.NaverApiException;
import com.team3.deokhugam.global.config.NaverProperties;
import com.team3.deokhugam.service.image.ImageOptimizer;
import com.team3.deokhugam.service.image.ImageOptimizer.OptimizedImage;

import java.util.Base64;
import java.util.Optional;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

public class NaverBookClientTest {

  private static final String BOOK_API_URL = "https://openapi.naver.com/v1/search/book.json";
  private static final String CLIENT_ID = "naver-client-id";
  private static final String CLIENT_SECRET = "naver-client-secret";

  private RestTemplate restTemplate;
  private MockRestServiceServer server;
  private ImageOptimizer imageOptimizer;
  private NaverBookClient naverBookClient;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    server = MockRestServiceServer.createServer(restTemplate);

    imageOptimizer = org.mockito.Mockito.mock(ImageOptimizer.class);

    naverBookClient =
        new NaverBookClient(
            restTemplate,
            new NaverProperties(CLIENT_ID, CLIENT_SECRET, BOOK_API_URL, 3000, 1),
            imageOptimizer
        );
  }

  @Test
  @DisplayName("ISBN으로 네이버 도서 API를 호출하고 응답을 반환한다")
  void searchByIsbn() {
    // given
    String isbn = "9788965402602";

    String responseJson = """
        {
          "lastBuildDate": "Tue, 09 Jun 2026 00:00:00 +0900",
          "total": 1,
          "start": 1,
          "display": 1,
          "items": [
            {
              "title": "스프링 부트와 AWS로 혼자 구현하는 웹 서비스",
              "link": "https://example.com/book",
              "image": "https://example.com/book.jpg",
              "author": "이동욱",
              "discount": "19800",
              "publisher": "프리렉",
              "isbn": "8965402609 9788965402602",
              "description": "스프링 부트 실습서",
              "pubdate": "20191129"
            }
          ]
        }
        """;

    server.expect(once(), requestTo(
            BOOK_API_URL + "?query=9788965402602&display=1&start=1&sort=sim"))
        .andExpect(method(HttpMethod.GET))
        .andExpect(header("X-Naver-Client-Id", CLIENT_ID))
        .andExpect(header("X-Naver-Client-Secret", CLIENT_SECRET))
        .andRespond(withSuccess(responseJson, MediaType.APPLICATION_JSON));

    // when
    NaverBookSearchDto result = naverBookClient.searchByIsbn(isbn);

    // then
    assertThat(result).isNotNull();
    assertThat(result.total()).isEqualTo(1);
    assertThat(result.items()).hasSize(1);
    assertThat(result.items().get(0).isbn()).contains(isbn);
    assertThat(result.items().get(0).title()).isEqualTo("스프링 부트와 AWS로 혼자 구현하는 웹 서비스");

    server.verify();
  }

  @Test
  @DisplayName("네이버 API 설정값이 비어 있으면 예외가 발생한다")
  void searchByIsbnWithMissingProperties() {
    // given
    NaverBookClient client =
        new NaverBookClient(
            restTemplate,
            new NaverProperties("", CLIENT_SECRET, BOOK_API_URL, 3000, 1),
            imageOptimizer
        );

    // when, then
    assertThatThrownBy(() -> client.searchByIsbn("9788965402602"))
        .isInstanceOf(NaverApiException.class);
  }

  @Test
  @DisplayName("네이버 API 호출이 실패하면 예외가 발생한다")
  void searchByIsbnWithApiFailure() {
    // given
    String isbn = "9788965402602";

    server.expect(once(), requestTo(
        BOOK_API_URL + "?query=9788965402602&display=1&start=1&sort=sim"))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    // when, then
    assertThatThrownBy(() -> naverBookClient.searchByIsbn(isbn))
        .isInstanceOf(NaverApiException.class);

    server.verify();
  }

  @Test
  @DisplayName("네이버 썸네일 URL이 비어 있으면 null을 반환한다")
  void downloadImageAsBase64WithBlankUrl() {
    // when
    String result = naverBookClient.downloadImageAsBase64(" ");

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("네이버 썸네일 URL 형식이 올바르지 않으면 null을 반환한다")
  void downloadImageAsBase64WithInvalidUrl() {
    // when
    String result = naverBookClient.downloadImageAsBase64("http://[invalid-url");

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("네이버 썸네일 URL 스킴이 http 또는 https가 아니면 null을 반환한다")
  void downloadImageAsBase64WithUnsupportedScheme() {
    // when
    String result = naverBookClient.downloadImageAsBase64("ftp://example.com/book.jpg");

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("네이버 썸네일 다운로드가 실패하면 null을 반환한다")
  void downloadImageAsBase64WithDownloadFailure() {
    // given
    String imageUrl = "https://example.com/book.jpg";

    server.expect(once(), requestTo(imageUrl))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withServerError());

    // when
    String result = naverBookClient.downloadImageAsBase64(imageUrl);

    // then
    assertThat(result).isNull();

    server.verify();
  }

  @Test
  @DisplayName("네이버 썸네일 다운로드에 성공하면 Base64 문자열을 반환한다")
  void downloadImageAsBase64WithSuccess() {
    // given
    String imageUrl = "https://example.com/book.jpg";
    byte[] downloadedImageBytes = "downloaded-image".getBytes();
    byte[] optimizedImageBytes = "optimized-image".getBytes();

    server.expect(once(), requestTo(imageUrl))
        .andExpect(method(HttpMethod.GET))
        .andRespond(withSuccess(downloadedImageBytes, MediaType.IMAGE_JPEG));

    when(imageOptimizer.optimize(
        any(byte[].class),
        anyString(),
        anyString(),
        anyLong()
    )).thenReturn(
        Optional.of(
            new OptimizedImage(
                optimizedImageBytes,
                "book.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                optimizedImageBytes.length
            )
        )
    );

    // when
    String result = naverBookClient.downloadImageAsBase64(imageUrl);

    // then
    assertThat(result).isEqualTo(Base64.getEncoder().encodeToString(optimizedImageBytes));

    server.verify();
  }

  private byte[] createJpegImageBytes(int width, int height) throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();

    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);
      graphics.setColor(Color.BLACK);
      graphics.drawString("ISBN 978-89-6540-260-2", 10, 20);
    } finally {
      graphics.dispose();
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", outputStream);

    return outputStream.toByteArray();
  }
}
