package com.team3.deokhugam.dto.review;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import java.util.UUID;
import org.springframework.data.domain.Sort;

public record ReviewSearchRequest(
    UUID userId,
    UUID bookId,
    String keyword,
    ReviewOrderBy orderBy,
    Sort.Direction direction,
    String cursor,
    int limit,
    UUID requestUserId
) {

  private static final ReviewOrderBy DEFAULT_ORDER_BY = ReviewOrderBy.CREATED_AT;
  private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;
  private static final int DEFAULT_LIMIT = 50;


  public static ReviewSearchRequest of(
      UUID userId,
      UUID bookId,
      String keyword,
      ReviewOrderBy orderBy,
      Sort.Direction direction,
      String cursor,
      Integer limit,
      UUID requestUserId
  ) {
    return new ReviewSearchRequest(
        userId,
        bookId,
        normalizeBlank(keyword),
        orderBy != null ? orderBy : DEFAULT_ORDER_BY,
        direction != null ? direction : DEFAULT_DIRECTION,
        normalizeBlank(cursor),
        parseLimit(limit),
        requestUserId
    );
  }

  public ReviewSearchRequest withLimit(int limit) {
    return new ReviewSearchRequest(
        userId, bookId, keyword, orderBy, direction,
        cursor, parseLimit(limit), requestUserId
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
    return cursor != null && !cursor.isBlank();
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