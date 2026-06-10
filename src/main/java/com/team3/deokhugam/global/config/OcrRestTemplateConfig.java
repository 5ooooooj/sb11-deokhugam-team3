package com.team3.deokhugam.global.config;

import java.time.Duration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
@EnableConfigurationProperties(OcrProperties.class)
public class OcrRestTemplateConfig {

  @Bean
  public RestTemplate ocrRestTemplate(RestTemplateBuilder builder, OcrProperties properties) {
    Duration timeout = Duration.ofMillis(properties.timeout());

    return builder.connectTimeout(timeout).readTimeout(timeout).build();
  }
}
