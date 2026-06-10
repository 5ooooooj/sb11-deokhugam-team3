package com.team3.deokhugam.dto.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class ReviewSearchRequestTest {

  private final UUID userId = UUID.randomUUID();

  @Test
  @DisplayName("orderBy/direction이 null이면 기본값(CREATED_AT/DESC)을 사용한다")
  void shouldApplyDefaultsForOrderByAndDirection() {
    ReviewSearchRequest req = ReviewSearchRequest.of(
        null, null, null, null, null, null, null, userId);

    assertThat(req.orderBy()).isEqualTo(ReviewOrderBy.CREATED_AT);
    assertThat(req.direction()).isEqualTo(Sort.Direction.DESC);
  }

  @Test
  @DisplayName("limit이 null이면 기본값 50을 사용한다")
  void shouldApplyDefaultLimitWhenNull() {
    ReviewSearchRequest req = ReviewSearchRequest.of(
        null, null, null, null, null, null, null, userId);

    assertThat(req.limit()).isEqualTo(50);
  }

  @Test
  @DisplayName("limit이 0 이하면 INVALID_INPUT 예외를 던진다")
  void shouldRejectNonPositiveLimit() {
    assertThatThrownBy(() -> ReviewSearchRequest.of(
        null, null, null, null, null, null, 0, userId))
        .isInstanceOf(DeokhugamException.class)
        .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);
  }

  @Test
  @DisplayName("blank keyword/cursor는 null로 정규화된다")
  void shouldNormalizeBlankStrings() {
    ReviewSearchRequest req = ReviewSearchRequest.of(
        null, null, "   ", null, null, "   ", null, userId);

    assertThat(req.keyword()).isNull();
    assertThat(req.cursor()).isNull();
    assertThat(req.hasKeyword()).isFalse();
    assertThat(req.hasCursor()).isFalse();
  }

  @Test
  @DisplayName("cursor 값이 있으면 hasCursor가 true")
  void hasCursorReturnsTrueWhenSet() {
    ReviewSearchRequest req = ReviewSearchRequest.of(
        null, null, null, null, null, "someToken", null, userId);

    assertThat(req.hasCursor()).isTrue();
  }

  @Test
  @DisplayName("withLimit은 다른 필드는 유지하고 limit만 바꾼 새 인스턴스를 만든다")
  void withLimitKeepsOtherFields() {
    ReviewSearchRequest original = ReviewSearchRequest.of(
        null, null, "keyword", ReviewOrderBy.RATING, Sort.Direction.ASC,
        "cursor", 10, userId);

    ReviewSearchRequest updated = original.withLimit(99);

    assertThat(updated.limit()).isEqualTo(99);
    assertThat(updated.keyword()).isEqualTo("keyword");
    assertThat(updated.orderBy()).isEqualTo(ReviewOrderBy.RATING);
    assertThat(updated.direction()).isEqualTo(Sort.Direction.ASC);
    assertThat(updated.cursor()).isEqualTo("cursor");
    assertThat(updated.requestUserId()).isEqualTo(userId);
  }
}