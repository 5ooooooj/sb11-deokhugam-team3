package com.team3.deokhugam.client.naver;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.team3.deokhugam.client.naver.dto.NaverBookSearchDto;
import com.team3.deokhugam.exception.naver.NaverApiException;
import com.team3.deokhugam.global.config.NaverProperties;
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
  private NaverBookClient naverBookClient;

  @BeforeEach
  void setUp() {
    restTemplate = new RestTemplate();
    server = MockRestServiceServer.createServer(restTemplate);

    naverBookClient =
        new NaverBookClient(
            restTemplate,
            new NaverProperties(CLIENT_ID, CLIENT_SECRET, BOOK_API_URL, 3000, 1)
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
            new NaverProperties("", CLIENT_SECRET, BOOK_API_URL, 3000, 1)
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
}
