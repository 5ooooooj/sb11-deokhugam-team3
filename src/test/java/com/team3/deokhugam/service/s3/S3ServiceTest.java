package com.team3.deokhugam.service.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.exception.s3.S3DeleteException;
import com.team3.deokhugam.exception.s3.S3UploadException;
import com.team3.deokhugam.global.config.AwsProperties;
import java.net.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.mock.web.MockMultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Utilities;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectResponse;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // ① UnnecessaryStubbingException 해결
public class S3ServiceTest {

  @Mock
  private S3Client s3Client;

  @Mock
  private AwsProperties props;

  @Mock
  private AwsProperties.S3 s3Props;

  @InjectMocks
  private S3Service s3Service;

  private static final String BUCKET = "test-bucket";
  private static final String REGION = "ap-northeast-2";
  private static final String KEY = "thumbnails/test.jpg";

  @BeforeEach
  void setUp() throws Exception {
    given(props.getS3()).willReturn(s3Props);
    given(s3Props.getBucket()).willReturn(BUCKET);
    given(props.getRegion()).willReturn(REGION);

    // ② s3Client.utilities() NPE 해결
    S3Utilities mockUtilities = mock(S3Utilities.class);
    given(s3Client.utilities()).willReturn(mockUtilities);
    given(mockUtilities.getUrl(any(GetUrlRequest.class)))
        .willReturn(new URL("https://" + BUCKET + ".s3." + REGION + ".amazonaws.com/" + KEY));
  }

  @Test
  @DisplayName("성공: S3 업로드")
  void 파일_업로드_성공() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "image", "test.jpg", "image/jpeg", "test".getBytes()
    );

    String url = s3Service.upload(file, KEY);

    verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    assertThat(url).contains(BUCKET);
    assertThat(url).contains(KEY);
  }

  @Test
  @DisplayName("실패: S3 업로드 실패시 예외 발생")
  void 파일_업로드_실패시_예외발생() throws Exception {
    MockMultipartFile file = new MockMultipartFile(
        "image", "test.jpg", "image/jpeg", "test-content".getBytes()
    );
    given(s3Client.putObject(any(PutObjectRequest.class), any(RequestBody.class)))
        .willThrow(S3Exception.builder().message("S3 오류").build());

    assertThatThrownBy(() -> s3Service.upload(file, KEY))
        .isInstanceOf(S3UploadException.class);
  }

  @Test
  @DisplayName("성공: S3 업로드된 파일 삭제")
  void 파일_삭제_성공() throws Exception {
    // ③ deleteObject()는 void가 아니라 DeleteObjectResponse 반환 → doNothing() 사용 불가
    given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .willReturn(DeleteObjectResponse.builder().build());

    assertThatCode(() -> s3Service.delete(KEY))
        .doesNotThrowAnyException();
    verify(s3Client).deleteObject(any(DeleteObjectRequest.class));
  }

  @Test
  @DisplayName("실패: S3 업로드된 파일 삭제 실패시 예외 발생")
  void 파일_삭제_실패시_예외발생() {
    given(s3Client.deleteObject(any(DeleteObjectRequest.class)))
        .willThrow(S3Exception.builder().message("S3 오류").build());

    assertThatThrownBy(() -> s3Service.delete(KEY))
        .isInstanceOf(S3DeleteException.class);
  }
}