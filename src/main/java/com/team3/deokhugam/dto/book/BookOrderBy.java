package com.team3.deokhugam.dto.book;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
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

  @JsonValue
  public String getValue() {
    return value;
  }

  @JsonCreator
  public static BookOrderBy from(String value) {
    if (value == null || value.isBlank()) {
      return TITLE;
    }

    String normalizedValue = value.trim();

    return Arrays.stream(values())
        .filter(orderBy -> orderBy.value.equals(normalizedValue)
            || orderBy.name().equalsIgnoreCase(normalizedValue)
        )
        .findFirst()
        .orElseThrow(InvalidBookSearchConditionException::new);
  }
}
