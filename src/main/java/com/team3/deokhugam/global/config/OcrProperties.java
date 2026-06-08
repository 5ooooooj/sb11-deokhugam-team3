package com.team3.deokhugam.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.ocr")
public record OcrProperties(
    String apiKey,
    String apiUrl,
    int timeout,
    int retry
) {

}
