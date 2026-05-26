package com.team3.deokhugam.global.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cloud.aws")
@Getter
@Setter
public class AwsProperties {

    private String region;
    private S3 s3 = new S3();

    @Getter
    @Setter
    public static class S3 {
      private String bucket;
    }
}
