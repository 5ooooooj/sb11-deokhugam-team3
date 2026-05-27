package com.team3.deokhugam.service.s3;

import com.team3.deokhugam.exception.s3.EmptyFileUploadException;
import com.team3.deokhugam.exception.s3.InvalidRequestException;
import com.team3.deokhugam.exception.s3.S3DeleteException;
import com.team3.deokhugam.exception.s3.S3UploadException;
import com.team3.deokhugam.global.config.AwsProperties;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@Slf4j
@RequiredArgsConstructor
public class S3Service {

  private final S3Client s3Client;
  private final AwsProperties props;

  public String upload(MultipartFile file, String key) {
    log.info("파일 업로드 시작 - 요청 KEY: {}", key);
    if (file == null || file.isEmpty()) {
      log.info("빈 파일 업로드 시도 발생");
      throw new EmptyFileUploadException();
    }
    if (key == null || key.isBlank()) {
      log.info("유효하지 않은 S3 업로드 키 전달됨");
      throw new InvalidRequestException();
    }

    try {
      s3Client.putObject(
          PutObjectRequest.builder()
              .bucket(props.getS3().getBucket())
              .key(key)
              .contentType(file.getContentType())
              .build(),
          RequestBody.fromInputStream(file.getInputStream(), file.getSize())
      );
      return generateUrl(key);
    } catch (S3Exception | IOException e) {
      log.error("S3 파일 업로드 실패 - KEY: {}", key, e);
      throw new S3UploadException();
    }
  }

  public void delete(String key) {
    log.info("S3 파일 삭제 시작 - 요청 KEY: {}", key);

    if (key == null || key.isBlank()) {
      log.info("유효하지 않은 S3 삭제 키 전달됨");
      throw new InvalidRequestException();
    }

    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(props.getS3().getBucket())
              .key(key)
              .build()
      );
      log.info("S3 파일 삭제 완료 - KEY: {}", key);
    } catch (S3Exception e) {
      log.error("S3 파일 삭제 실패 - KEY: {}", key, e);
      throw new S3DeleteException();
    }
  }

  private String generateUrl(String key) {
    return s3Client.utilities()
        .getUrl(GetUrlRequest.builder()
            .bucket(props.getS3().getBucket())
            .key(key)
            .build())
        .toString();
  }

}
