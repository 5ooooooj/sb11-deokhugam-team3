package com.team3.deokhugam.client.ocr;

import com.team3.deokhugam.client.ocr.dto.OcrResultDto;
import com.team3.deokhugam.exception.ocr.InvalidOcrImageException;
import com.team3.deokhugam.exception.ocr.OcrApiException;
import com.team3.deokhugam.global.config.OcrProperties;
import java.io.IOException;
import java.io.InputStream;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.InputStreamResource;
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

  private final RestTemplate restTemplate;
  private final OcrProperties properties;

  public OcrClient(
      @Qualifier("ocrRestTemplate") RestTemplate restTemplate,
      OcrProperties properties
  ) {
    this.restTemplate = restTemplate;
    this.properties = properties;
  }

  public OcrResultDto parseImage(MultipartFile image) {
    validateProperties();

    HttpHeaders headers = new HttpHeaders();
    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
    headers.set(OCR_API_KEY_HEADER, properties.apiKey());

    MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
    body.add(FILE_PART_NAME, toFileResource(image));
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
          "OCR Space API 호출 실패 - apiUrl: {}, filename: {}, size: {}, contentType: {}",
          properties.apiUrl(),
          image.getOriginalFilename(),
          image.getSize(),
          image.getContentType(),
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

  private MultipartInputStreamFileResource toFileResource(MultipartFile image) {
    try {
      return new MultipartInputStreamFileResource(
          image.getInputStream(),
          image.getOriginalFilename(),
          image.getSize()
      );
    } catch (IOException e) {
      throw new InvalidOcrImageException(e);
    }
  }

  private static class MultipartInputStreamFileResource extends InputStreamResource {
    private final String filename;
    private final long contentLength;

    public MultipartInputStreamFileResource(InputStream inputStream, String filename, long contentLength) {
      super(inputStream);
      this.filename = StringUtils.hasText(filename) ? filename : "ocr-image";
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
  }
}
