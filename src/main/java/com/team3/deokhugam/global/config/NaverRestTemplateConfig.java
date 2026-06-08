package com.team3.deokhugam.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

// #70에서 RestTemplate 방식으로 먼저 구현한 후,
// 다음 이슈에서 RestClient 방식으로 교체 예정입니다.
@Configuration
@EnableConfigurationProperties(NaverProperties.class)
public class NaverRestTemplateConfig {

  @Bean
  public RestTemplate naverRestTemplate(RestTemplateBuilder builder, NaverProperties properties) {
    Duration timeout = Duration.ofMillis(properties.timeout());

    return builder.connectTimeout(timeout).readTimeout(timeout).build();
  }
}
