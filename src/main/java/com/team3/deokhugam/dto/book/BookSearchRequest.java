package com.team3.deokhugam.dto.book;

import java.time.Instant;
import org.springframework.data.domain.Sort;

public record BookSearchRequest(
    String keyword,
    BookOrderBy orderBy,
    Sort.Direction direction,
    String cursor,
    Instant after,
    int limit
) {

  private static final int DEFAULT_LIMIT = 50;

  public static BookSearchRequest of(
      String keyword,
      String orderBy,
      String direction,
      String cursor,
      Instant after,
      int limit
  ) {
    return new BookSearchRequest(
        normalizeBlank(keyword),
        BookOrderBy.from(orderBy),
        parseDirection(direction),
        normalizeBlank(cursor),
        after,
        parseLimit(limit)
    );
  }

  private static Sort.Direction parseDirection(String direction) {
    if (direction == null || direction.isBlank()) {
      return Sort.Direction.DESC;
    }

    return Sort.Direction.fromString(direction);
  }

  private static int parseLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }

    if (limit <= 0) {
      throw new IllegalArgumentException("페이지의 크기는 1이상이어야 합니다.");
    }

    return limit;
  }

  private  static String normalizeBlank(String value) {
    if (value == null || value.isBlank()) {
      return null;
    }

    return value.trim();
  }

  public boolean hasKeyword() {
    return keyword != null;
  }

  public boolean hasCursor() {
    return cursor != null && after != null;
  }
}
