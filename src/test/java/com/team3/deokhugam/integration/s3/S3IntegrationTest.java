package com.team3.deokhugam.integration.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team3.deokhugam.exception.s3.S3UploadException;
import com.team3.deokhugam.global.config.AwsProperties;
import com.team3.deokhugam.service.s3.S3Service;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import software.amazon.awssdk.services.s3.S3Client;

@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "AWS_ACCESS_KEY_ID", matches = ".+")
@EnabledIfEnvironmentVariable(named = "AWS_SECRET_ACCESS_KEY", matches = ".+")
@Slf4j
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class S3IntegrationTest {

  @Autowired
  private S3Client s3Client;

  @Autowired
  private AwsProperties props;

  @Autowired
  private S3Service s3Service;

  private static final String KEY = "thumbnails/test-" + UUID.randomUUID() + ".jpg";

  @Test
  @Order(1)
  @DisplayName("성공: S3 파일 업로드")
  void 파일_업로드_성공() {
    // given
    MockMultipartFile file = new MockMultipartFile(
        "image", "test.jpg", "image/jpeg", "test=content".getBytes()
    );

    // when
    String url = s3Service.upload(file, KEY);

    // then
    log.info("업로드된 URL: {}", url);
    assertThat(url).contains(props.getS3().getBucket());
    assertThat(url).contains(KEY);
  }

  @Test
  @Order(2)
  @DisplayName("성공: S3 파일 삭제")
  void 파일_삭제_성공() {
    // when & then
    assertThatCode(() -> s3Service.delete(KEY))
        .doesNotThrowAnyException();
  }

  @Test
  @Order(3)
  @DisplayName("실패: 존재하지 않는 키 삭제 시도")
  void 존재하지_않는_파일_삭제() {
    // given
    String nonExistentKey = "thumbnails/non-existent-" + UUID.randomUUID() + ".jpg";

    // when & then
    // s3는 존재하지 않는 키 삭제해도 예외 안 던짐 -> 정상 동작 확인
    assertThatCode(() -> s3Service.delete(nonExistentKey))
        .doesNotThrowAnyException();
  }

  @Test
  @Order(4)
  @DisplayName("실패: 빈 파일 업로드 시도")
  void 빈_파일_업로드_실패() {
    // given
    MockMultipartFile emptyFile = new MockMultipartFile(
        "image", "empty.jpg", "image/jpeg", new byte[0]
    );

    // when & then
    assertThatThrownBy(() -> s3Service.upload(emptyFile, KEY))
        .isInstanceOf(S3UploadException.class);
  }

}
