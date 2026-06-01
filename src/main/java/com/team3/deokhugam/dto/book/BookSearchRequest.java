package com.team3.deokhugam.dto.book;

import com.team3.deokhugam.exception.book.InvalidBookSearchConditionException;
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

  private static final BookOrderBy DEFAULT_ORDER_BY = BookOrderBy.TITLE;
  private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;
  private static final int DEFAULT_LIMIT = 50;
  private static final int MAX_LIMIT = 100;

  public static BookSearchRequest of(
      String keyword,
      String orderBy,
      String direction,
      String cursor,
      Instant after,
      Integer limit
  ) {
    return new BookSearchRequest(
        normalizeBlank(keyword),
        parseOrderBy(orderBy),
        parseDirection(direction),
        normalizeBlank(cursor),
        after,
        parseLimit(limit)
    );
  }


  public BookSearchRequest withLimit(int limit) {
    return new BookSearchRequest(
        keyword,
        orderBy,
        direction,
        cursor,
        after,
        limit
    );
  }

  public boolean hasKeyword() {
    return keyword != null && !keyword.isBlank();
  }

  public boolean hasCursor() {
    boolean hasCursorValue = cursor != null && !cursor.isBlank();

    if(!hasCursorValue && after == null) {
      return false;
    }

    if(!hasCursorValue || after == null) {
      throw new InvalidBookSearchConditionException();
    }

    return true;
  }

  private static BookOrderBy parseOrderBy(String orderBy) {
    if (orderBy == null || orderBy.isBlank()) {
      return DEFAULT_ORDER_BY;
    }

    return BookOrderBy.from(orderBy);
  }

  private static Sort.Direction parseDirection(String direction) {
    if (direction == null || direction.isBlank()) {
      return DEFAULT_DIRECTION;
    }

    try {
      return Sort.Direction.fromString(direction);
    } catch (IllegalArgumentException e) {
      throw new InvalidBookSearchConditionException();
    }
  }

  private static int parseLimit(Integer limit) {
    if (limit == null) {
      return DEFAULT_LIMIT;
    }

    if (limit <= 0 || limit > MAX_LIMIT) {
      throw new InvalidBookSearchConditionException();
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
