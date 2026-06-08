package com.team3.deokhugam.client.naver;

import com.team3.deokhugam.client.naver.dto.NaverBookSearchDto;
import com.team3.deokhugam.exception.naver.NaverApiException;
import com.team3.deokhugam.global.config.NaverProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Slf4j
@Component
public class NaverBookClient {

  private static final String NAVER_CLIENT_ID_HEADER = "X-Naver-Client-Id";
  private static final String NAVER_CLIENT_SECRET_HEADER = "X-Naver-Client-Secret";
  private static final int DISPLAY_ONE = 1;
  private static final int START_FIRST = 1;
  private static final String SORT_SIMILARITY = "sim";

  private final RestTemplate restTemplate;
  private final NaverProperties properties;

  public NaverBookClient(
      @Qualifier("naverRestTemplate") RestTemplate restTemplate,
      NaverProperties properties
  ) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  public NaverBookSearchDto searchByIsbn(String isbn) {
    validateProperties();

    String uri = UriComponentsBuilder
        .fromUriString(properties.bookApiUrl())
        .queryParam("query", isbn)
        .queryParam("display", DISPLAY_ONE)
        .queryParam("start", START_FIRST)
        .queryParam("sort", SORT_SIMILARITY)
        .build()
        .encode()
        .toUriString();

    HttpHeaders headers = new HttpHeaders();
    headers.set(NAVER_CLIENT_ID_HEADER, properties.clientId());
    headers.set(NAVER_CLIENT_SECRET_HEADER, properties.clientSecret());

    try {
      ResponseEntity<NaverBookSearchDto> response = restTemplate.exchange(
          uri, HttpMethod.GET, new HttpEntity<>(headers), NaverBookSearchDto.class
      );
      return response.getBody();
    } catch (RestClientException e) {
      log.warn("네이버 도서 API 호출 실패 - isbn: {}", isbn, e);
      throw new NaverApiException();
    }
  }

  private void validateProperties() {
    if (!StringUtils.hasText(properties.clientId())
        || !StringUtils.hasText(properties.clientSecret())
        || !StringUtils.hasText(properties.bookApiUrl())
    ) {
      throw new NaverApiException();
    }
  }
}
