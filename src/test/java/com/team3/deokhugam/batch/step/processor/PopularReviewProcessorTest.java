package com.team3.deokhugam.batch.step.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PopularReviewProcessorTest {

  private PopularReviewProcessor popularReviewProcessor;

  @BeforeEach
  void setUp() {
    popularReviewProcessor = new PopularReviewProcessor();
  }

  @Test
  @DisplayName("성공: 좋아요 수와 댓글 수로 점수를 계산")
  void process_success() throws Exception {
    // given - score = (10 * 0.3) + (5 * 0.7) = 3.0 + 3.5 = 6.5
    PopularReviewRawData rawData = new PopularReviewRawData(
        UUID.randomUUID(), 10, 5
    );

    // when
    PopularReview result = popularReviewProcessor.create(Period.DAILY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(BigDecimal.valueOf(6.5), within(BigDecimal.valueOf(0.001)));
    assertThat(result.getPeriod()).isEqualTo(Period.DAILY);
    assertThat(result.getReviewId()).isEqualTo(rawData.reviewId());
    assertThat(result.getLikeCount()).isEqualTo(10);
    assertThat(result.getCommentCount()).isEqualTo(5);
  }

  @Test
  @DisplayName("성공: 좋아요만 있을 때 점수를 계산")
  void process_onlyLikes() throws Exception {
    // given - score = (5 * 0.3) + (0 * 0.7) = 1.5
    PopularReviewRawData rawData = new PopularReviewRawData(
        UUID.randomUUID(), 5, 0
    );

    // when
    PopularReview result = popularReviewProcessor.create(Period.WEEKLY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(BigDecimal.valueOf(1.5), within(BigDecimal.valueOf(0.001)));
    assertThat(result.getPeriod()).isEqualTo(Period.WEEKLY);
  }

  @Test
  @DisplayName("성공: 댓글만 있을 때 점수를 계산")
  void process_onlyComments() throws Exception {
    // given - score = (0 * 0.3) + (5 * 0.7) = 3.5
    PopularReviewRawData rawData = new PopularReviewRawData(
        UUID.randomUUID(), 0, 5
    );

    // when
    PopularReview result = popularReviewProcessor.create(Period.MONTHLY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(BigDecimal.valueOf(3.5), within(BigDecimal.valueOf(0.001)));
    assertThat(result.getPeriod()).isEqualTo(Period.MONTHLY);
  }

  @Test
  @DisplayName("성공: 좋아요, 댓글 모두 0일 때 점수가 0")
  void process_zeroAll() throws Exception {
    // given - score = 0
    PopularReviewRawData rawData = new PopularReviewRawData(
        UUID.randomUUID(), 0, 0
    );

    // when
    PopularReview result = popularReviewProcessor.create(Period.ALL_TIME).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getPeriod()).isEqualTo(Period.ALL_TIME);
  }

  @Test
  @DisplayName("성공: 댓글 가중치가 좋아요보다 높음을 검증")
  void process_commentWeightHigherThanLike() throws Exception {
    // given - 좋아요만: (10 * 0.3) = 3.0 vs 댓글만: (10 * 0.7) = 7.0
    PopularReviewRawData likeOnly = new PopularReviewRawData(UUID.randomUUID(), 10, 0);
    PopularReviewRawData commentOnly = new PopularReviewRawData(UUID.randomUUID(), 0, 10);

    PopularReview likeResult = popularReviewProcessor.create(Period.DAILY).process(likeOnly);
    PopularReview commentResult = popularReviewProcessor.create(Period.DAILY).process(commentOnly);

    assertThat(commentResult.getScore()).isGreaterThan(likeResult.getScore());
  }

  @Test
  @DisplayName("성공: Period가 올바르게 설정됨")
  void process_periodIsSet() throws Exception {
    PopularReviewRawData rawData = new PopularReviewRawData(
        UUID.randomUUID(), 1, 1
    );

    for (Period period : Period.values()) {
      PopularReview result = popularReviewProcessor.create(period).process(rawData);
      assertThat(result).isNotNull();
      assertThat(result.getPeriod()).isEqualTo(period);
    }
  }
}
