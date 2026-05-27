package com.team3.deokhugam.dto.book;

import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.exception.book.InvalidBookSearchConditionException;
import java.util.Arrays;

public enum BookOrderBy {

  TITLE("title"),
  PUBLISHED_DATE("publishedDate"),
  RATING("rating"),
  REVIEW_COUNT("reviewCount");

  private final String value;

  BookOrderBy(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static BookOrderBy from(String value) {
    if (value == null || value.isBlank()) {
      return TITLE;
    }

    return Arrays.stream(values())
        .filter(orderBy -> orderBy.value.equals(value))
        .findFirst()
        .orElseThrow(() -> new InvalidBookSearchConditionException("지원하지 않는 도서 정렬 기준입니다."));
  }
}
