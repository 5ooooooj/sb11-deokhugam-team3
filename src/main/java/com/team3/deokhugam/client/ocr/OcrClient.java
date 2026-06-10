package com.team3.deokhugam.client.ocr;

import com.team3.deokhugam.client.ocr.dto.OcrResultDto;
import com.team3.deokhugam.exception.ocr.InvalidOcrImageException;
import com.team3.deokhugam.exception.ocr.OcrApiException;
import com.team3.deokhugam.global.config.OcrProperties;
import com.team3.deokhugam.service.image.ImageOptimizer;
import com.team3.deokhugam.service.image.ImageOptimizer.OptimizedImage;
import java.io.ByteArrayInputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Component
public class OcrClient {

  private static final String OCR_API_KEY_HEADER = "apikey";
  private static final String FILE_PART_NAME = "file";
  private static final String LANGUAGE_PART_NAME = "language";
  private static final String IS_OVERLAY_REQUIRED_PART_NAME = "isOverlayRequired";
  private static final String DETECT_ORIENTATION_PART_NAME = "detectOrientation";
  private static final String SCALE_PART_NAME = "scale";
  private static final String OCR_ENGINE_PART_NAME = "OCREngine";

  private static final String LANGUAGE_ENGLISH = "eng";
  private static final String FALSE = "false";
  private static final String TRUE = "true";
  private static final String OCR_ENGINE_2 = "2";
  private static final long MAX_OCR_IMAGE_SIZE_BYTES = 1_500_000L;

  private final RestTemplate restTemplate;
  private final OcrProperties properties;
  private final ImageOptimizer imageOptimizer;

  public OcrClient(
      @Qualifier("ocrRestTemplate") RestTemplate restTemplate,
      OcrProperties properties,
      ImageOptimizer imageOptimizer
  ) {
    this.restTemplate = restTemplate;
    this.properties = properties;
    this.imageOptimizer = imageOptimizer;
  }

  public OcrResultDto parseImage(MultipartFile image) {
    validateProperties();

    OptimizedImage optimizedImage = optimizeImageForOcr(image);

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.set(OCR_API_KEY_HEADER, properties.apiKey());

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(FILE_PART_NAME, toFileResource(optimizedImage));
    body.add(LANGUAGE_PART_NAME, LANGUAGE_ENGLISH);
    body.add(IS_OVERLAY_REQUIRED_PART_NAME, FALSE);
    body.add(DETECT_ORIENTATION_PART_NAME, TRUE);
    body.add(SCALE_PART_NAME, TRUE);
    body.add(OCR_ENGINE_PART_NAME, OCR_ENGINE_2);

    try {
      return restTemplate.postForObject(
          properties.apiUrl(),
          new HttpEntity<>(body, headers),
          OcrResultDto.class
      );
    } catch (RestClientException e) {
      log.warn(
          "OCR Space API 호출 실패 - apiUrl: {}, filename: {}, originalSize: {}, uploadSize: {}, contentType: {}",
          properties.apiUrl(),
          image.getOriginalFilename(),
          image.getSize(),
          optimizedImage.size(),
          optimizedImage.contentType(),
          e
      );
      throw new OcrApiException();
    }
  }

  private void validateProperties() {
    if (!StringUtils.hasText(properties.apiKey())
        || !StringUtils.hasText(properties.apiUrl())
    ) {
      throw new OcrApiException();
    }
  }

  private OptimizedImage optimizeImageForOcr(MultipartFile image) {
    return imageOptimizer.optimize(image, MAX_OCR_IMAGE_SIZE_BYTES)
        .orElseThrow(() -> {
          log.warn(
              "OCR 이미지 압축 실패 또는 크기 제한 초과 - filename: {}, size: {}, max: {}, contentType: {}",
              image.getOriginalFilename(),
              image.getSize(),
              MAX_OCR_IMAGE_SIZE_BYTES,
              image.getContentType()
          );
          return new InvalidOcrImageException();
        });
  }

  private MultipartByteArrayResource toFileResource(OptimizedImage image) {
    return new MultipartByteArrayResource(
        image.bytes(),
        image.filename(),
        image.size()
    );
  }

  private static class MultipartByteArrayResource extends ByteArrayResource {

    private final String filename;
    private final long contentLength;

    public MultipartByteArrayResource(byte[] byteArray, String filename, long contentLength) {
      super(byteArray);
      this.filename = StringUtils.hasText(filename) ? filename : "ocr-image.jpg";
      this.contentLength = contentLength;
    }

    @Override
    public String getFilename() {
      return filename;
    }

    @Override
    public long contentLength() {
      return contentLength;
    }

    @Override
    public ByteArrayInputStream getInputStream() {
      return new ByteArrayInputStream(getByteArray());
    }
  }
}