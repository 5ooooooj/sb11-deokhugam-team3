package com.team3.deokhugam.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "external.ocr")
public record OcrProperties(
    String apiKey,
    String apiUrl,
    int timeout,
    int retry
) {

  public OcrProperties {
    if (!StringUtils.hasText(apiUrl)) {
      throw new IllegalArgumentException("external.ocr.api-url은 공백일 수 없습니다.");
    }

    if (timeout <= 0) {
      throw new IllegalArgumentException("external.ocr.timeout 반드시 0보다 커야 합니다.");
    }

    if (retry < 0) {
      throw new IllegalArgumentException("external.ocr.retry 음수 설정은 잘못된 값입니다.");
    }

    // retry는 추후 RestClient 전환 시 사용 예정입니다.
    // TODO
  }
}
