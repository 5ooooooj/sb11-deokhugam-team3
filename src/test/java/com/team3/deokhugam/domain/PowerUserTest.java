package com.team3.deokhugam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PowerUserTest {

  @Test
  @DisplayName("성공: assignRank로 rank가 변경된다")
  void assignRank_success() {
    // given
    PowerUser powerUser = PowerUser.builder()
        .userId(UUID.randomUUID())
        .ranking(0)
        .score(BigDecimal.valueOf(90))
        .reviewScoreSum(BigDecimal.valueOf(100))
        .likeCount(10)
        .period(Period.DAILY)
        .calculatedAt(Instant.now())
        .commentCount(10)
        .build();

    // when
    powerUser.assignRank(1);

    // then
    assertThat(powerUser.getRanking()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: assignRank를 여러 번 호출하면 마지막 값으로 변경된다")
  void assignRank_overwrite() {
    // given
    PowerUser powerUser = PowerUser.builder()
        .userId(UUID.randomUUID())
        .ranking(0)
        .score(BigDecimal.valueOf(90))
        .reviewScoreSum(BigDecimal.valueOf(100))
        .likeCount(10)
        .period(Period.DAILY)
        .calculatedAt(Instant.now())
        .commentCount(10)
        .build();

    // when
    powerUser.assignRank(5);

    // then
    assertThat(powerUser.getRanking()).isEqualTo(5);
  }
}
