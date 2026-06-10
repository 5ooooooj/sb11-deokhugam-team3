package com.team3.deokhugam.integration.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import com.team3.deokhugam.global.config.AwsProperties;
import com.team3.deokhugam.service.s3.S3Service;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S3IntegrationTest {

  @Container
  static LocalStackContainer localstack =
      new LocalStackContainer(DockerImageName.parse("localstack/localstack:3"))
          .withServices(LocalStackContainer.Service.S3);

  @Autowired
  private S3Service s3Service;

  @Autowired
  private AwsProperties props;

  private static final String KEY = "thumbnails/test-" + UUID.randomUUID() + ".jpg";

  @TestConfiguration
  static class LocalStackS3Config {

    @Bean
    @Primary
    public S3Client localStackS3Client() {
      return S3Client.builder()
          .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create(
                      localstack.getAccessKey(),
                      localstack.getSecretKey()
                  )
              )
          )
          .region(Region.of(localstack.getRegion()))
          .forcePathStyle(true)  // LocalStack 필수
          .build();
    }

    @Bean
    @Primary
    public S3Presigner localStackS3Presigner() {
      return S3Presigner.builder()
          .endpointOverride(localstack.getEndpointOverride(LocalStackContainer.Service.S3))
          .credentialsProvider(
              StaticCredentialsProvider.create(
                  AwsBasicCredentials.create(
                      localstack.getAccessKey(),
                      localstack.getSecretKey()
                  )
              )
          )
          .region(Region.of(localstack.getRegion()))
          .build();
    }
  }

  @BeforeAll
  static void setUpBucket(@Autowired S3Client s3Client, @Autowired AwsProperties props) {
    s3Client.createBucket(r -> r.bucket(props.getS3().getBucket()));
  }

  @Test
  @Order(1)
  @DisplayName("성공: S3 파일 업로드")
  void 파일_업로드_성공() {
    MockMultipartFile file = new MockMultipartFile(
        "image", "test.jpg", "image/jpeg", "test-content".getBytes()
    );

    String url = s3Service.upload(file, KEY);

    log.info("업로드된 URL: {}", url);
    assertThat(url).contains(props.getS3().getBucket());
    assertThat(url).contains(KEY);
  }

  @Test
  @Order(2)
  @DisplayName("성공: S3 파일 삭제")
  void 파일_삭제_성공() {
    assertThatCode(() -> s3Service.delete(KEY))
        .doesNotThrowAnyException();
  }

  @Test
  @Order(3)
  @DisplayName("실패: 존재하지 않는 키 삭제 시도")
  void 존재하지_않는_파일_삭제() {
    String nonExistentKey = "thumbnails/non-existent-" + UUID.randomUUID() + ".jpg";

    // S3는 존재하지 않는 키 삭제해도 예외 안 던짐
    assertThatCode(() -> s3Service.delete(nonExistentKey))
        .doesNotThrowAnyException();
  }
}
