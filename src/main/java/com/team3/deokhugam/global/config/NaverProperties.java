package com.team3.deokhugam.global.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "external.naver")
public record NaverProperties(
    String clientId,
    String clientSecret,
    String bookApiUrl,
    int timeout,
    int retry
) {

}
