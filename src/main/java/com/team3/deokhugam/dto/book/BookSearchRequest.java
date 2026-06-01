package com.team3.deokhugam.dto.book;

import com.team3.deokhugam.exception.book.InvalidBookSearchConditionException;
import java.time.Instant;
import org.springframework.data.domain.Sort;

public record BookSearchRequest(
    String keyword,
    BookOrderBy orderBy,
    Sort.Direction direction,
    BookCursor cursor,
    int limit
) {

  private static final BookOrderBy DEFAULT_ORDER_BY = BookOrderBy.TITLE;
  private static final Sort.Direction DEFAULT_DIRECTION = Sort.Direction.DESC;
  private static final int DEFAULT_LIMIT = 50;
  private static final int MAX_LIMIT = 100;

  public static BookSearchRequest of(
      String keyword,
      BookOrderBy orderBy,
      Sort.Direction direction,
      String cursor,
      Integer limit
  ) {
    return new BookSearchRequest(
        normalizeBlank(keyword),
        resolveOrderBy(orderBy),
        resolveDirection(direction),
        parseCursor(cursor),
        parseLimit(limit)
    );
  }


  public BookSearchRequest withLimit(int limit) {
    return new BookSearchRequest(
        keyword,
        orderBy,
        direction,
        cursor,
        limit
    );
  }

  public boolean hasKeyword() {
    return keyword != null && !keyword.isBlank();
  }

  public boolean hasCursor() {
    return cursor != null;
  }

  private static BookOrderBy resolveOrderBy(BookOrderBy orderBy) {
    if (orderBy == null) {
      return DEFAULT_ORDER_BY;
    }

    return orderBy;
  }

  private static Sort.Direction resolveDirection(Sort.Direction direction) {
    if (direction == null) {
      return DEFAULT_DIRECTION;
    }
    return direction;
  }

  private static BookCursor parseCursor(String cursor) {
    if (cursor == null || cursor.isBlank()) {
      return null;
    }

    return BookCursor.decode(cursor);
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
