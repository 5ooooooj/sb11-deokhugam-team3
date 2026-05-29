package com.team3.deokhugam.dto.review;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.domain.Sort;

public record ReviewSearchRequest(
    UUID userId,
    UUID bookId,
    String keyword,
    ReviewOrderBy orderBy,
    Sort.Direction direction,
    String cursor,
    Instant after,
    int limit,
    UUID requestUserId
) {

  private static final ReviewOrderBy DEFAULT_ORDER_BY = ReviewOrderBy.CREATED_AT;
  private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;
  private static final int DEFAULT_LIMIT = 50;
  private static void validateCursorAndAfter(String cursor, Instant after) {
    boolean hasCursor = cursor != null && !cursor.isBlank();
    boolean hasAfter = after != null;
    if (hasCursor != hasAfter) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }

  public static ReviewSearchRequest of(
      UUID userId,
      UUID bookId,
      String keyword,
      String orderBy,
      String direction,
      String cursor,
      Instant after,
      Integer limit,
      UUID requestUserId
  ) {
    validateCursorAndAfter(cursor, after);
    return new ReviewSearchRequest(
        userId,
        bookId,
        normalizeBlank(keyword),
        parseOrderBy(orderBy),
        parseDirection(direction),
        normalizeBlank(cursor),
        after,
        parseLimit(limit),
        requestUserId
    );
  }

  public ReviewSearchRequest withLimit(int limit) {
    return new ReviewSearchRequest(
        userId, bookId, keyword, orderBy, direction,
        cursor, after, parseLimit(limit), requestUserId
    );
  }

  public boolean hasUserId() {
    return userId != null;
  }

  public boolean hasBookId() {
    return bookId != null;
  }

  public boolean hasKeyword() {
    return keyword != null && !keyword.isBlank();
  }

  public boolean hasCursor() {
    return cursor != null && after != null && !cursor.isBlank();
  }

  private static ReviewOrderBy parseOrderBy(String orderBy) {
    if (orderBy == null || orderBy.isBlank()) {
      return DEFAULT_ORDER_BY;
    }
    return ReviewOrderBy.from(orderBy);
  }

  private static Sort.Direction parseDirection(String direction) {
    if (direction == null || direction.isBlank()) {
      return DEFAULT_DIRECTION;
    }
    try {
      return Sort.Direction.fromString(direction);
    } catch (IllegalArgumentException e) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }

  private static int parseLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }
    if (limit <= 0) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
    return limit;
  }

  private static String normalizeBlank(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }
    return value.trim();
  }
}