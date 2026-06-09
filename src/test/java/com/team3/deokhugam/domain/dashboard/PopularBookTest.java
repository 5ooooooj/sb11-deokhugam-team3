package com.team3.deokhugam.domain.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PopularBookTest {

  @Test
  @DisplayName("성공: assignRank로 rank가 변경된다")
  void assignRank_success() {
    // given
    PopularBook popularBook = PopularBook.builder()
        .bookId(UUID.randomUUID())
        .period(Period.DAILY)
        .score(BigDecimal.valueOf(90))
        .ranking(0)
        .reviewCount(10)
        .rating(BigDecimal.valueOf(4.5))
        .calculatedAt(Instant.now())
        .build();

    // when
    popularBook.assignRank(1);

    // then
    assertThat(popularBook.getRanking()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: assignRank를 여러 번 호출하면 마지막 값으로 변경된다")
  void assignRank_overwrite() {
    // given
    PopularBook popularBook = PopularBook.builder()
        .bookId(UUID.randomUUID())
        .period(Period.DAILY)
        .score(BigDecimal.valueOf(90))
        .ranking(1)
        .reviewCount(10)
        .rating(BigDecimal.valueOf(4.5))
        .calculatedAt(Instant.now())
        .build();

    // when
    popularBook.assignRank(5);

    // then
    assertThat(popularBook.getRanking()).isEqualTo(5);
  }
}
