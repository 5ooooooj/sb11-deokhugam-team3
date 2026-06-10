package com.team3.deokhugam.batch.step.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PowerUserProcessorTest {

  private PowerUserProcessor powerUserProcessor;

  @BeforeEach
  void setUp() {
    powerUserProcessor = new PowerUserProcessor();
  }

  @Test
  @DisplayName("성공: reviewScoreSum, 좋아요, 댓글로 점수를 계산")
  void process_success() throws Exception {
    // given - score = (4.0 * 0.5) + (2 * 0.2) + (1 * 0.3) = 2.0 + 0.4 + 0.3 = 2.7
    PowerUserRawData rawData = new PowerUserRawData(
        UUID.randomUUID(), BigDecimal.valueOf(4.0), 2, 1
    );

    // when
    PowerUser result = powerUserProcessor.create(Period.DAILY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(BigDecimal.valueOf(2.7), within(BigDecimal.valueOf(0.001)));
    assertThat(result.getPeriod()).isEqualTo(Period.DAILY);
    assertThat(result.getUserId()).isEqualTo(rawData.userId());
    assertThat(result.getReviewScoreSum()).isCloseTo(BigDecimal.valueOf(4.0), within(BigDecimal.valueOf(0.001)));
    assertThat(result.getLikeCount()).isEqualTo(2);
    assertThat(result.getCommentCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 리뷰 점수만 있을 때 점수를 계산")
  void process_onlyReviewScore() throws Exception {
    // given - score = (5.0 * 0.5) + (0 * 0.2) + (0 * 0.3) = 2.5
    PowerUserRawData rawData = new PowerUserRawData(
        UUID.randomUUID(), BigDecimal.valueOf(5.0), 0, 0
    );

    // when
    PowerUser result = powerUserProcessor.create(Period.WEEKLY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(BigDecimal.valueOf(2.5), within(BigDecimal.valueOf(0.001)));
    assertThat(result.getPeriod()).isEqualTo(Period.WEEKLY);
  }

  @Test
  @DisplayName("성공: 좋아요만 있을 때 점수를 계산")
  void process_onlyLikes() throws Exception {
    // given - score = (0 * 0.5) + (10 * 0.2) + (0 * 0.3) = 2.0
    PowerUserRawData rawData = new PowerUserRawData(
        UUID.randomUUID(), BigDecimal.ZERO, 10, 0
    );

    // when
    PowerUser result = powerUserProcessor.create(Period.MONTHLY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(BigDecimal.valueOf(2.0), within(BigDecimal.valueOf(0.001)));
    assertThat(result.getPeriod()).isEqualTo(Period.MONTHLY);
  }

  @Test
  @DisplayName("성공: 댓글만 있을 때 점수를 계산")
  void process_onlyComments() throws Exception {
    // given - score = (0 * 0.5) + (0 * 0.2) + (10 * 0.3) = 3.0
    PowerUserRawData rawData = new PowerUserRawData(
        UUID.randomUUID(), BigDecimal.ZERO, 0, 10
    );

    // when
    PowerUser result = powerUserProcessor.create(Period.ALL_TIME).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(BigDecimal.valueOf(3.0), within(BigDecimal.valueOf(0.001)));
    assertThat(result.getPeriod()).isEqualTo(Period.ALL_TIME);
  }

  @Test
  @DisplayName("성공: 모든 값이 0일 때 점수가 0")
  void process_zeroAll() throws Exception {
    // given
    PowerUserRawData rawData = new PowerUserRawData(
        UUID.randomUUID(), BigDecimal.ZERO, 0, 0
    );

    // when
    PowerUser result = powerUserProcessor.create(Period.DAILY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  @DisplayName("성공: 가중치 순서 검증 - reviewScore > comment > like")
  void process_weightOrder() throws Exception {
    // reviewScore(0.5) > comment(0.3) > like(0.2) 동일 값 10 기준
    PowerUserRawData reviewOnly = new PowerUserRawData(UUID.randomUUID(), BigDecimal.valueOf(10.0), 0, 0);
    PowerUserRawData commentOnly = new PowerUserRawData(UUID.randomUUID(), BigDecimal.ZERO, 0, 10);
    PowerUserRawData likeOnly = new PowerUserRawData(UUID.randomUUID(), BigDecimal.ZERO, 10, 0);

    PowerUser reviewResult = powerUserProcessor.create(Period.DAILY).process(reviewOnly);
    PowerUser commentResult = powerUserProcessor.create(Period.DAILY).process(commentOnly);
    PowerUser likeResult = powerUserProcessor.create(Period.DAILY).process(likeOnly);

    assertThat(reviewResult.getScore()).isGreaterThan(commentResult.getScore());
    assertThat(commentResult.getScore()).isGreaterThan(likeResult.getScore());
  }

  @Test
  @DisplayName("성공: Period가 올바르게 설정됨")
  void process_periodIsSet() throws Exception {
    PowerUserRawData rawData = new PowerUserRawData(
        UUID.randomUUID(), BigDecimal.valueOf(1.0), 1, 1
    );

    for (Period period : Period.values()) {
      PowerUser result = powerUserProcessor.create(period).process(rawData);
      assertThat(result).isNotNull();
      assertThat(result.getPeriod()).isEqualTo(period);
    }
  }
}