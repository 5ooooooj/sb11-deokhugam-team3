package com.team3.deokhugam.service.image;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.service.image.ImageOptimizer.OptimizedImage;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
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
  @DisplayName("확장자가 없는 파일명은 jpg 확장자를 붙인다")
  void optimize_withFilenameWithoutExtension_convertsToJpgFilename() throws Exception {
    // given
    byte[] imageBytes = createJpegImageBytes(2000, 2000);
    long maxBytes = Math.max(20_000L, imageBytes.length / 3);

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
  }
}