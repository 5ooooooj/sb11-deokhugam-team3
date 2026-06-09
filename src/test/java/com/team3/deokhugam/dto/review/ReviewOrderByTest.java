package com.team3.deokhugam.dto.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ReviewOrderByTest {

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {" ", "  "})
  @DisplayName("null/공백 입력 시 CREATED_AT 기본값을 돌려준다")
  void shouldReturnDefaultWhenBlank(String input) {
    assertThat(ReviewOrderBy.from(input)).isEqualTo(ReviewOrderBy.CREATED_AT);
  }

  @ParameterizedTest
  @ValueSource(strings = {"createdAt", "CREATED_AT", "createdat", "CreatedAt"})
  @DisplayName("createdAt 표기와 enum 이름 모두 CREATED_AT으로 변환된다")
  void shouldMatchCreatedAtCaseInsensitively(String input) {
    assertThat(ReviewOrderBy.from(input)).isEqualTo(ReviewOrderBy.CREATED_AT);
  }

  @ParameterizedTest
  @ValueSource(strings = {"rating", "RATING", "Rating"})
  @DisplayName("rating 표기와 enum 이름 모두 RATING으로 변환된다")
  void shouldMatchRatingCaseInsensitively(String input) {
    assertThat(ReviewOrderBy.from(input)).isEqualTo(ReviewOrderBy.RATING);
  }

  @Test
  @DisplayName("알 수 없는 값은 INVALID_INPUT 예외를 던진다")
  void shouldThrowOnUnknown() {
    assertThatThrownBy(() -> ReviewOrderBy.from("nope"))
        .isInstanceOf(DeokhugamException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
  }
}