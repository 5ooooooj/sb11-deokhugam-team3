package com.team3.deokhugam.service.image;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Iterator;
import java.util.Optional;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Component
public class ImageOptimizer {

  private static final String DEFAULT_FILENAME = "image.jpg";
  private static final String JPEG_CONTENT_TYPE = "image/jpeg";
  private static final String JPEG_FORMAT = "jpg";

  private static final int[] TARGET_WIDTHS = {1600, 1200, 1000, 800, 600};
  private static final float[] JPEG_QUALITIES = {0.85f, 0.75f, 0.65f, 0.55f, 0.45f, 0.35f};

  public Optional<OptimizedImage> optimize(MultipartFile image, long maxBytes) {
    if (image == null || image.isEmpty()) {
      return Optional.empty();
    }

    try {
      return optimize(
          image.getBytes(),
          image.getOriginalFilename(),
          image.getContentType(),
          maxBytes
      );
    } catch (Exception e) {
      return Optional.empty();
    }
  }

  public Optional<OptimizedImage> optimize(
      byte[] imageBytes,
      String originalFilename,
      String contentType,
      long maxBytes
  ) {
    if (imageBytes == null || imageBytes.length == 0) {
      return Optional.empty();
    }

    if (imageBytes.length <= maxBytes) {
      return Optional.of(
          new OptimizedImage(
              imageBytes,
              resolveFilename(originalFilename),
              resolveContentType(contentType),
              imageBytes.length
          )
      );
    }

    BufferedImage sourceImage;

    try {
      sourceImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
    } catch (Exception e) {
      return Optional.empty();
    }

    if (sourceImage == null) {
      return Optional.empty();
    }

    OptimizedImage smallestCandidate = null;

    // 가장 작은 이미지가 아니라, 제한 용량 이하를 만족하는 고품질 이미지를 우선 선택합니다.
    // TARGET_WIDTHS와 JPEG_QUALITIES는 큰 해상도/높은 품질 순서로 정렬되어 있으므로,
    // 첫 번째로 maxBytes 이하를 만족하는 결과를 반환하면 OCR 인식률과 썸네일 품질을 더 잘 유지할 수 있습니다.
    for (int targetWidth : TARGET_WIDTHS) {
      BufferedImage resizedImage = resizeIfNeeded(sourceImage, targetWidth);

      for (float quality : JPEG_QUALITIES) {
        byte[] compressedBytes = writeJpeg(resizedImage, quality);

        if (compressedBytes == null || compressedBytes.length == 0) {
          continue;
        }

        OptimizedImage optimizedImage =
            new OptimizedImage(
                compressedBytes,
                toJpegFilename(originalFilename),
                JPEG_CONTENT_TYPE,
                compressedBytes.length
            );

        if (smallestCandidate == null || optimizedImage.size() < smallestCandidate.size()) {
          smallestCandidate = optimizedImage;
        }

        if (optimizedImage.size() <= maxBytes) {
          return Optional.of(optimizedImage);
        }
      }
    }

    if (smallestCandidate != null && smallestCandidate.size() <= maxBytes) {
      return Optional.of(smallestCandidate);
    }

    return Optional.empty();
  }

  private BufferedImage resizeIfNeeded(BufferedImage sourceImage, int targetWidth) {
    BufferedImage rgbImage = toRgbImage(sourceImage);

    if (rgbImage.getWidth() <= targetWidth) {
      return rgbImage;
    }

    double ratio = (double) targetWidth / rgbImage.getWidth();
    int targetHeight = Math.max(1, (int) Math.round(rgbImage.getHeight() * ratio));

    BufferedImage resizedImage =
        new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);

    Graphics2D graphics = resizedImage.createGraphics();

    try {
      graphics.setRenderingHint(
          RenderingHints.KEY_INTERPOLATION,
          RenderingHints.VALUE_INTERPOLATION_BILINEAR
      );
      graphics.setRenderingHint(
          RenderingHints.KEY_RENDERING,
          RenderingHints.VALUE_RENDER_QUALITY
      );
      graphics.setRenderingHint(
          RenderingHints.KEY_ANTIALIASING,
          RenderingHints.VALUE_ANTIALIAS_ON
      );
      graphics.drawImage(rgbImage, 0, 0, targetWidth, targetHeight, null);
    } finally {
      graphics.dispose();
    }

    return resizedImage;
  }

  private BufferedImage toRgbImage(BufferedImage sourceImage) {
    if (sourceImage.getType() == BufferedImage.TYPE_INT_RGB) {
      return sourceImage;
    }

    BufferedImage rgbImage =
        new BufferedImage(sourceImage.getWidth(), sourceImage.getHeight(),
            BufferedImage.TYPE_INT_RGB);

    Graphics2D graphics = rgbImage.createGraphics();

    try {
      graphics.setColor(Color.WHITE);
      graphics.fillRect(0, 0, rgbImage.getWidth(), rgbImage.getHeight());
      graphics.drawImage(sourceImage, 0, 0, null);
    } finally {
      graphics.dispose();
    }

    return rgbImage;
  }

  private byte[] writeJpeg(BufferedImage image, float quality) {
    Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(JPEG_FORMAT);

    if (!writers.hasNext()) {
      return null;
    }

    ImageWriter writer = writers.next();

    try (
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)
    ) {
      writer.setOutput(imageOutputStream);

      ImageWriteParam writeParam = writer.getDefaultWriteParam();

      if (writeParam.canWriteCompressed()) {
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionQuality(quality);
      }

      writer.write(null, new IIOImage(image, null, null), writeParam);
      imageOutputStream.flush();

      return outputStream.toByteArray();
    } catch (Exception e) {
      return null;
    } finally {
      writer.dispose();
    }
  }

  private String resolveFilename(String originalFilename) {
    return StringUtils.hasText(originalFilename) ? originalFilename : DEFAULT_FILENAME;
  }

  private String resolveContentType(String contentType) {
    return StringUtils.hasText(contentType) ? contentType : JPEG_CONTENT_TYPE;
  }

  private String toJpegFilename(String originalFilename) {
    if (!StringUtils.hasText(originalFilename)) {
      return DEFAULT_FILENAME;
    }

    int extensionIndex = originalFilename.lastIndexOf('.');

    if (extensionIndex < 0) {
      return originalFilename + ".jpg";
    }

    return originalFilename.substring(0, extensionIndex) + ".jpg";
  }

  public record OptimizedImage(
      byte[] bytes,
      String filename,
      String contentType,
      long size
  ) {

  }
}