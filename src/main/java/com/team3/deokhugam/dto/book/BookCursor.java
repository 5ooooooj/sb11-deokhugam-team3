package com.team3.deokhugam.dto.book;

import com.team3.deokhugam.exception.book.InvalidBookSearchConditionException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Base64;
import java.util.UUID;

public record BookCursor(
    String value,
    Instant createdAt,
    UUID id
) {

  private static final String DELIMITER = "|";

  public static String encode(String value, Instant createdAt, UUID id) {
    if (value == null || value.isBlank() || createdAt == null || id == null) {
      throw new InvalidBookSearchConditionException();
    }

    String encodedValue = encodeBase64(value);

    String payload = String.join(
        DELIMITER,
        encodedValue,
        createdAt.toString(),
        id.toString()
    );

    return encodeBase64(payload);
  }

  public static BookCursor decode(String token) {
    if (token == null || token.isBlank()) {
      throw new InvalidBookSearchConditionException();
    }

    try {
      String payload = decodeBase64(token);
      String[] parts = payload.split("\\|", -1);

      if (parts.length != 3) {
        throw new InvalidBookSearchConditionException();
      }

      String value = decodeBase64(parts[0]);
      Instant createdAt = Instant.parse(parts[1]);
      UUID id = UUID.fromString(parts[2]);

      if (value.isBlank()) {
        throw new InvalidBookSearchConditionException();
      }

      return new BookCursor(value, createdAt, id);
    } catch (IllegalArgumentException | DateTimeParseException e) {
      throw new InvalidBookSearchConditionException();
    }
  }

  private static String encodeBase64(String value) {
    return Base64.getUrlEncoder()
        .withoutPadding()
        .encodeToString(value.getBytes(StandardCharsets.UTF_8));
  }

  private static String decodeBase64(String value) {
    return new String(
        Base64.getUrlDecoder().decode(value),
        StandardCharsets.UTF_8
    );
  }
}