package com.team3.deokhugam.service.image;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.service.image.ImageOptimizer.OptimizedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

class ImageOptimizerTest {

  private final ImageOptimizer imageOptimizer = new ImageOptimizer();

  @Test
  @DisplayName("제한 용량 이하 이미지는 원본을 그대로 반환한다")
  void optimize_withSmallImage_returnsOriginalImage() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(100, 100);

    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "small.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            imageBytes
        );

    // when
    Optional<OptimizedImage> result = imageOptimizer.optimize(image, imageBytes.length + 1);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().bytes()).isEqualTo(imageBytes);
    assertThat(result.get().filename()).isEqualTo("small.jpg");
    assertThat(result.get().contentType()).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
    assertThat(result.get().size()).isEqualTo(imageBytes.length);
  }

  @Test
  @DisplayName("제한 용량 초과 이미지는 압축해서 제한 용량 이하로 반환한다")
  void optimize_withLargeImage_returnsCompressedImage() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(2000, 2000);
    long maxBytes = Math.max(20_000L, imageBytes.length / 3);

    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "large.png",
            MediaType.IMAGE_PNG_VALUE,
            imageBytes
        );

    // when
    Optional<OptimizedImage> result = imageOptimizer.optimize(image, maxBytes);

    // then
    assertThat(result).isPresent();
    assertThat(result.get().size()).isLessThanOrEqualTo(maxBytes);
    assertThat(result.get().filename()).isEqualTo("large.jpg");
    assertThat(result.get().contentType()).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
    assertThat(result.get().bytes()).isNotEqualTo(imageBytes);
  }

  @Test
  @DisplayName("이미지가 아닌 바이트는 Optional.empty를 반환한다")
  void optimize_withInvalidImageBytes_returnsEmpty() {
    // given
    byte[] invalidBytes = "not-image".getBytes();

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            invalidBytes,
            "invalid.txt",
            MediaType.TEXT_PLAIN_VALUE,
            1_500_000L
        );

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("null 또는 빈 이미지는 Optional.empty를 반환한다")
  void optimize_withEmptyImage_returnsEmpty() {
    // given
    MockMultipartFile emptyImage =
        new MockMultipartFile(
            "image",
            "empty.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            new byte[0]
        );

    // when
    Optional<OptimizedImage> result = imageOptimizer.optimize(emptyImage, 1_500_000L);

    // then
    assertThat(result).isEmpty();
  }

  private byte[] createJpegImageBytes(int width, int height) throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    Graphics2D graphics = image.createGraphics();

    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, width, height);
      graphics.setColor(Color.BLACK);

      for (int y = 0; y < height; y += 40) {
        graphics.drawString("ISBN 978-89-6540-260-2 테스트 텍스트", 20, y + 20);
      }
    } finally {
      graphics.dispose();
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "jpg", outputStream);

    return outputStream.toByteArray();
  }

  @Test
  @DisplayName("byte 배열이 null이면 Optional.empty를 반환한다")
  void optimize_withNullBytes_returnsEmpty() {
    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            null,
            "book.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            1_500_000L
        );

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("파일명이 null이면 기본 파일명을 사용한다")
  void optimize_withNullFilename_usesDefaultFilename() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(100, 100);

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            imageBytes,
            null,
            MediaType.IMAGE_JPEG_VALUE,
            imageBytes.length + 1
        );

    // then
    assertThat(result).isPresent();
    assertThat(result.get().filename()).isEqualTo("image.jpg");
  }

  @Test
  @DisplayName("contentType이 null이면 기본 이미지 contentType을 사용한다")
  void optimize_withNullContentType_usesDefaultContentType() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(100, 100);

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            imageBytes,
            "book.jpg",
            null,
            imageBytes.length + 1
        );

    // then
    assertThat(result).isPresent();
    assertThat(result.get().contentType()).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
  }

  @Test
  @DisplayName("확장자가 없는 파일명은 압축 후 jpg 확장자를 붙인다")
  void optimize_withFilenameWithoutExtension_convertsToJpgFilename() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(2000, 2000);
    long maxBytes = imageBytes.length -1L;

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            imageBytes,
            "book-cover",
            MediaType.IMAGE_PNG_VALUE,
            maxBytes
        );

    // then
    assertThat(result).isPresent();
    assertThat(result.get().filename()).isEqualTo("book-cover.jpg");
    assertThat(result.get().contentType()).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
    assertThat(result.get().size()).isLessThanOrEqualTo(maxBytes);
  }

  @Test
  @DisplayName("투명 PNG 이미지는 JPEG 압축을 위해 RGB 이미지로 변환된다")
  void optimize_withTransparentPng_convertsToJpeg() throws Exception {
    // given
    byte[] imageBytes = createTransparentPngImageBytes(1200, 1200);
    long maxBytes = Math.max(10_000L, imageBytes.length - 1L);

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            imageBytes,
            "transparent.png",
            MediaType.IMAGE_PNG_VALUE,
            maxBytes
        );

    // then
    assertThat(result).isPresent();
    assertThat(result.get().filename()).isEqualTo("transparent.jpg");
    assertThat(result.get().contentType()).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
    assertThat(result.get().size()).isLessThanOrEqualTo(maxBytes);
  }

  @Test
  @DisplayName("MultipartFile 바이트 조회에 실패하면 Optional.empty를 반환한다")
  void optimize_withMultipartFileGetBytesFailure_returnsEmpty() {
    // given
    MockMultipartFile image =
        new MockMultipartFile(
            "image",
            "book.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            "test-image".getBytes()
        ) {
          @Override
          public byte[] getBytes() throws IOException {
            throw new IOException("파일 읽기 실패");
          }
        };

    // when
    Optional<OptimizedImage> result = imageOptimizer.optimize(image, 1_500_000L);

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("이미지 contentType이지만 실제 이미지 바이트가 아니면 Optional.empty를 반환한다")
  void optimize_withInvalidImageBytesAndImageContentType_returnsEmpty() {
    // given
    byte[] invalidImageBytes = "not-image".repeat(100).getBytes();

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            invalidImageBytes,
            "invalid.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            10L
        );

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("contentType이 대문자 IMAGE/JPEG여도 이미지로 처리한다")
  void optimize_withUpperCaseImageContentType_returnsOriginalImage() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(100, 100);

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            imageBytes,
            "book.jpg",
            "IMAGE/JPEG",
            imageBytes.length + 1L
        );

    // then
    assertThat(result).isPresent();
    assertThat(result.get().bytes()).isEqualTo(imageBytes);
    assertThat(result.get().filename()).isEqualTo("book.jpg");
    assertThat(result.get().contentType()).isEqualTo("IMAGE/JPEG");
    assertThat(result.get().size()).isEqualTo(imageBytes.length);
  }

  @Test
  @DisplayName("파일명이 공백이면 기본 파일명을 사용한다")
  void optimize_withBlankFilename_usesDefaultFilename() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(100, 100);

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            imageBytes,
            " ",
            MediaType.IMAGE_JPEG_VALUE,
            imageBytes.length + 1L
        );

    // then
    assertThat(result).isPresent();
    assertThat(result.get().filename()).isEqualTo("image.jpg");
    assertThat(result.get().contentType()).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
    assertThat(result.get().size()).isEqualTo(imageBytes.length);
  }

  @Test
  @DisplayName("압축이 필요한 이미지의 확장자는 jpg로 변환한다")
  void optimize_withPngFilename_convertsFilenameToJpgAfterCompression() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(2000, 2000);
    long maxBytes = imageBytes.length - 1L;

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            imageBytes,
            "book-cover.png",
            MediaType.IMAGE_PNG_VALUE,
            maxBytes
        );

    // then
    assertThat(result).isPresent();
    assertThat(result.get().filename()).isEqualTo("book-cover.jpg");
    assertThat(result.get().contentType()).isEqualTo(MediaType.IMAGE_JPEG_VALUE);
    assertThat(result.get().size()).isLessThanOrEqualTo(maxBytes);
  }

  @Test
  @DisplayName("압축 후에도 제한 용량 이하로 줄일 수 없으면 Optional.empty를 반환한다")
  void optimize_whenCannotCompressUnderMaxBytes_returnsEmpty() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(2000, 2000);

    // when
    Optional<OptimizedImage> result =
        imageOptimizer.optimize(
            imageBytes,
            "book.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            1L
        );

    // then
    assertThat(result).isEmpty();
  }

  private byte[] createTransparentPngImageBytes(int width, int height) throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
    Graphics2D graphics = image.createGraphics();

    try {
      graphics.setColor(new Color(255, 255, 255, 0));
      graphics.fillRect(0, 0, width, height);
      graphics.setColor(new Color(0, 0, 0, 180));

      for (int y = 0; y < height; y += 40) {
        graphics.drawString("ISBN 978-89-6540-260-2 테스트 텍스트", 20, y + 20);
      }
    } finally {
      graphics.dispose();
    }

    ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    ImageIO.write(image, "png", outputStream);

    return outputStream.toByteArray();
  }
}