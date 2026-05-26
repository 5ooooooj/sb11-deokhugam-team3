package com.team3.deokhugam.service;

import com.team3.deokhugam.global.config.AwsProperties;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetUrlRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Service
@RequiredArgsConstructor
public class S3Service {

  private final S3Client s3Client;
  private final AwsProperties props;

  public String upload(MultipartFile file, String key) {
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
      throw new S3UploadException();
    }
  }

  public void delete(String key) {
    try {
      s3Client.deleteObject(
          DeleteObjectRequest.builder()
              .bucket(props.getS3().getBucket())
              .key(key)
              .build()
      );
    } catch (S3Exception e) {
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
