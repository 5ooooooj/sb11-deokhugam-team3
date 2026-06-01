package com.team3.deokhugam.dto.book;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.team3.deokhugam.exception.book.InvalidBookSearchConditionException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class BookCursorTest {

  @Test
  @DisplayName("정렬값, 생성일, ID를 cursor token으로 인코딩하고 다시 디코딩할 수 있다.")
  void encodeAndDecode() {
    // given
    String sortValue = "자바";
    Instant createdAt = Instant.parse("2026-06-01T00:00:00Z");
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // when
    String token = BookCursor.encode(sortValue, createdAt, id);
    BookCursor result = BookCursor.decode(token);

    // then
    assertThat(result.value()).isEqualTo(sortValue);
    assertThat(result.createdAt()).isEqualTo(createdAt);
    assertThat(result.id()).isEqualTo(id);
  }

  @Test
  @DisplayName("정렬값에 구분자가 포함되어도 cursor token을 안전하게 디코딩할 수 있다.")
  void encodeAndDecodeWithDelimiterInValue() {
    // given
    String sortValue = "자바|스프링";
    Instant createdAt = Instant.parse("2026-06-01T00:00:00Z");
    UUID id = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // when
    String token = BookCursor.encode(sortValue, createdAt, id);
    BookCursor result = BookCursor.decode(token);

    // then
    assertThat(result.value()).isEqualTo(sortValue);
    assertThat(result.createdAt()).isEqualTo(createdAt);
    assertThat(result.id()).isEqualTo(id);
  }

  @Test
  @DisplayName("잘못된 cursor token이면 예외 발생")
  void decodeInvalidToken() {
    // when, then
    assertThatThrownBy(() -> BookCursor.decode("invalid-token"))
        .isInstanceOf(InvalidBookSearchConditionException.class);
  }

  @Test
  @DisplayName("cursor token이 null이면 예외 발생")
  void decodeNullToken() {
    // when, then
    assertThatThrownBy(() -> BookCursor.decode(null))
        .isInstanceOf(InvalidBookSearchConditionException.class);
  }

  @Test
  @DisplayName("cursor token이 공백이면 예외 발생")
  void decodeBlankToken() {
    // when, then
    assertThatThrownBy(() -> BookCursor.decode(" "))
        .isInstanceOf(InvalidBookSearchConditionException.class);
  }
}
