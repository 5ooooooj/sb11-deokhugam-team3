package com.team3.deokhugam.dto.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team3.deokhugam.exception.book.InvalidBookSearchConditionException;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

class BookSearchRequestTest {

  @Test
  @DisplayName("limit이 null이면 기본값 50을 사용한다")
  void defaultLimit() {
    // given, when
    BookSearchRequest request =
        BookSearchRequest.of(
            null,
            null,
            null,
            null,
            null,
            null
        );

    // then
    assertThat(request.limit()).isEqualTo(50);
  }

  @Test
  @DisplayName("limit이 100이면 허용한다")
  void maxLimit() {
    // given, when
    BookSearchRequest request =
        BookSearchRequest.of(
            null,
            null,
            null,
            null,
            null,
            100
        );

    // then
    assertThat(request.limit()).isEqualTo(100);
  }

  @Test
  @DisplayName("limit이 100을 초과하면 예외 발생")
  void limitGreaterThanMax() {
    // when, then
    assertThatThrownBy(() ->
        BookSearchRequest.of(
            null,
            null,
            null,
            null,
            null,
            101
        )
    ).isInstanceOf(InvalidBookSearchConditionException.class);
  }

  @Test
  @DisplayName("limit이 0 이하이면 예외 발생")
  void limitLessThanOrEqualZero() {
    // when, then
    assertThatThrownBy(() ->
        BookSearchRequest.of(
            null,
            null,
            null,
            null,
            null,
            0
        )
    ).isInstanceOf(InvalidBookSearchConditionException.class);
  }

  @Test
  @DisplayName("cursor와 after가 모두 없으면 첫 페이지 요청이다")
  void hasCursorWithNoCursorAndNoAfter() {
    // given
    BookSearchRequest request =
        BookSearchRequest.of(
            null,
            null,
            null,
            null,
            null,
            50
        );

    // when, then
    assertThat(request.hasCursor()).isFalse();
  }

  @Test
  @DisplayName("cursor와 after가 모두 있으면 커서 요청이다")
  void hasCursorWithCursorAndAfter() {
    // given
    Instant after = Instant.parse("2026-06-01T00:00:00Z");

    BookSearchRequest request =
        BookSearchRequest.of(
            null,
            "title",
            "ASC",
            "자바",
            after,
            50
        );

    // when, then
    assertThat(request.hasCursor()).isTrue();
  }

  @Test
  @DisplayName("cursor만 있고 after가 없으면 예외 발생")
  void hasCursorWithCursorOnly() {
    // given
    BookSearchRequest request =
        BookSearchRequest.of(
            null,
            "title",
            "ASC",
            "자바",
            null,
            50
        );

    // when, then
    assertThatThrownBy(request::hasCursor)
        .isInstanceOf(InvalidBookSearchConditionException.class);
  }

  @Test
  @DisplayName("after만 있고 cursor가 없으면 예외 발생")
  void hasCursorWithAfterOnly() {
    // given
    Instant after = Instant.parse("2026-06-01T00:00:00Z");

    BookSearchRequest request =
        BookSearchRequest.of(
            null,
            "title",
            "ASC",
            null,
            after,
            50
        );

    // when, then
    assertThatThrownBy(request::hasCursor)
        .isInstanceOf(InvalidBookSearchConditionException.class);
  }

  @Test
  @DisplayName("cursor가 공백이고 after가 있으면 예외 발생")
  void hasCursorWithBlankCursorAndAfter() {
    // given
    Instant after = Instant.parse("2026-06-01T00:00:00Z");

    BookSearchRequest request =
        BookSearchRequest.of(
            null,
            "title",
            "ASC",
            " ",
            after,
            50
        );

    // when, then
    assertThatThrownBy(request::hasCursor)
        .isInstanceOf(InvalidBookSearchConditionException.class);
  }

  @Test
  @DisplayName("withLimit은 Service 내부 limit + 1 조회를 허용한다")
  void withLimitAllowsInternalLimitPlusOne() {
    // given
    BookSearchRequest request =
        BookSearchRequest.of(
            null,
            "title",
            "ASC",
            null,
            null,
            100
        );

    // when
    BookSearchRequest pageRequest = request.withLimit(request.limit() + 1);

    // then
    assertThat(pageRequest.limit()).isEqualTo(101);
    assertThat(pageRequest.orderBy()).isEqualTo(BookOrderBy.TITLE);
    assertThat(pageRequest.direction()).isEqualTo(Sort.Direction.ASC);
  }
}