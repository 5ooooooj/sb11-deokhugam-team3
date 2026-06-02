package com.team3.deokhugam.dto.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team3.deokhugam.exception.global.DeokhugamException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReviewOrderByConverterTest {

  private final ReviewOrderByConverter converter = new ReviewOrderByConverter();

  @Test
  @DisplayName("명세 표기(createdAt/rating)를 enum으로 변환한다")
  void shouldConvertLowerCamelValue() {
    assertThat(converter.convert("createdAt")).isEqualTo(ReviewOrderBy.CREATED_AT);
    assertThat(converter.convert("rating")).isEqualTo(ReviewOrderBy.RATING);
  }

  @Test
  @DisplayName("enum 이름(CREATED_AT/RATING)으로도 변환된다 (defaultValue 호환)")
  void shouldConvertEnumName() {
    assertThat(converter.convert("CREATED_AT")).isEqualTo(ReviewOrderBy.CREATED_AT);
    assertThat(converter.convert("RATING")).isEqualTo(ReviewOrderBy.RATING);
  }

  @Test
  @DisplayName("알 수 없는 값은 예외를 던진다 (Spring이 400으로 변환)")
  void shouldThrowOnUnknown() {
    assertThatThrownBy(() -> converter.convert("garbage"))
        .isInstanceOf(DeokhugamException.class);
  }
}