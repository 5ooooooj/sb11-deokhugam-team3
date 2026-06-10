package com.team3.deokhugam.client.naver;

import com.team3.deokhugam.client.naver.dto.NaverBookSearchDto;
import com.team3.deokhugam.exception.naver.NaverApiException;
import com.team3.deokhugam.global.config.NaverProperties;
import com.team3.deokhugam.service.image.ImageOptimizer;
import java.net.URI;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
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
  private static final long MAX_THUMBNAIL_IMAGE_SIZE_BYTES = 1_500_000L;

  private final RestTemplate restTemplate;
  private final NaverProperties properties;
  private final ImageOptimizer imageOptimizer;

  public NaverBookClient(
      @Qualifier("naverRestTemplate") RestTemplate restTemplate,
      NaverProperties properties, ImageOptimizer imageOptimizer
  ) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.imageOptimizer = imageOptimizer;
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

  public String downloadImageAsBase64(String imageUrl) {
    if (!StringUtils.hasText(imageUrl)) {
      return null;
    }

    URI imageUri;

    try {
      imageUri = URI.create(imageUrl);
    } catch (IllegalArgumentException e) {
      log.warn("네이버 썸네일 URL 형식이 올바르지 않습니다. imageUrl={}", imageUrl, e);
      return null;
    }

    String scheme = imageUri.getScheme();

    if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme)) {
      log.warn("지원하지 않는 네이버 썸네일 URL 스킴입니다. imageUrl={}", imageUrl);
      return null;
    }

    try {
      ResponseEntity<byte[]> response = restTemplate.getForEntity(imageUri, byte[].class);

      if (!response.getStatusCode().is2xxSuccessful()) {
        log.warn(
            "네이버 썸네일이미지 다운로드 실패 - status: {}, imageUrl: {}",
            response.getStatusCode(), imageUrl
        );
        return null;
      }

      byte[] imageBytes = response.getBody();

      if (imageBytes == null || imageBytes.length == 0) {
        log.warn("네이버 썸네일 이미지 응답이 비어 있습니다. imageUrl: {}", imageUrl);
        return null;
      }

      MediaType contentType = response.getHeaders().getContentType();

      return imageOptimizer
          .optimize(
              imageBytes,
              extractFilename(imageUri),
              contentType == null ? null : contentType.toString(),
              MAX_THUMBNAIL_IMAGE_SIZE_BYTES
          )
          .map(optimizedImage -> Base64.getEncoder().encodeToString(optimizedImage.bytes()))
          .orElseGet(() -> {
            log.warn(
                "네이버 썸네일 이미지 압축 실패 또는 크기 제한 초과 - originalSiz: {}, max: {}, imageUrl: {}",
                imageBytes.length, MAX_THUMBNAIL_IMAGE_SIZE_BYTES, imageUrl
            );
            return null;
          });
    } catch (RestClientException e) {
      log.warn("네이버 썸네일 이미지 다운로드 실패 - imageUrl: {}", imageUrl, e);
      return null;
    }
  }

  private String extractFilename(URI imageUri) {
    String path = imageUri.getPath();

    if (!StringUtils.hasText(path)) {
      return "naver-thumbnail.jpg";
    }

    int lastSlashIndex = path.lastIndexOf('/');

    if (lastSlashIndex < 0 || lastSlashIndex == path.length() - 1) {
      return "naver-thumbnail.jpg";
    }

    return path.substring(lastSlashIndex + 1);
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
