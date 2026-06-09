package com.team3.deokhugam.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class PopularReviewTest {

  @Test
  @DisplayName("성공: assignRank로 rank가 변경된다")
  void assignRank_success() {
    // given
    PopularReview popularReview = PopularReview.builder()
        .reviewId(UUID.randomUUID())
        .ranking(0)
        .likeCount(3)
        .commentCount(10)
        .score(BigDecimal.valueOf(90))
        .period(Period.DAILY)
        .calculatedAt(Instant.now())
        .build();

    // when
    popularReview.assignRank(1);

    // then
    assertThat(popularReview.getRanking()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: assignRank를 여러 번 호출하면 마지막 값으로 변경된다")
  void assignRank_overwrite() {
    // given
    PopularReview popularReview = PopularReview.builder()
        .reviewId(UUID.randomUUID())
        .ranking(0)
        .likeCount(3)
        .commentCount(10)
        .score(BigDecimal.valueOf(90))
        .period(Period.DAILY)
        .calculatedAt(Instant.now())
        .build();

    // when
    popularReview.assignRank(5);

    // then
    assertThat(popularReview.getRanking()).isEqualTo(5);
  }
}
