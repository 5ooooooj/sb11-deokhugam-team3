package com.team3.deokhugam.dto.review;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import java.util.Arrays;

public enum ReviewOrderBy {

  CREATED_AT("createdAt"),
  RATING("rating");

  private final String value;

  ReviewOrderBy(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static ReviewOrderBy from(String value) {
    if (value == null || value.isBlank()) {
      return CREATED_AT;
    }

    return Arrays.stream(values())
        .filter(orderBy -> orderBy.value.equals(value))
        .findFirst()
        .orElseThrow(() -> new DeokhugamException(ErrorCode.INVALID_INPUT));
  }
}