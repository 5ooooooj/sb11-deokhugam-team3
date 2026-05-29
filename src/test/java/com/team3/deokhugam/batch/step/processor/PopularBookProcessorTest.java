package com.team3.deokhugam.batch.step.processor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.offset;

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.PopularBook;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class PopularBookProcessorTest {

  private PopularBookProcessor popularBookProcessor;

  @BeforeEach
  void setUp() {
    popularBookProcessor = new PopularBookProcessor();
  }

  @Test
  @DisplayName("성공: 리뷰 수와 평점으로 점수를 게산")
  void process_success() throws Exception {
    // given
    PopularBookRawData rawData = new PopularBookRawData(
        UUID.randomUUID(),
        10,
        4.0
    );

    // when
    PopularBook result = popularBookProcessor.create(Period.DAILY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(6.4, offset(0.001));
    assertThat(result.getPeriod()).isEqualTo(Period.DAILY);
    assertThat(result.getBookId()).isEqualTo(rawData.bookId());
    assertThat(result.getReviewCount()).isEqualTo(10);
    assertThat(result.getRating()).isCloseTo(4.0, offset(0.001));
  }

  @Test
  @DisplayName("성공: 리뷰 수가 0일 때 평점만으로 점수를 계산")
  void process_zeroReviewCount() throws Exception {
    // given
    PopularBookRawData rawData = new PopularBookRawData(
        UUID.randomUUID(),
        0,
        3.0
    );

    // when
    PopularBook result = popularBookProcessor.create(Period.WEEKLY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(1.8, offset(0.001));
    assertThat(result.getPeriod()).isEqualTo(Period.WEEKLY);
  }

  @Test
  @DisplayName("평점이 최소값(1)일 때 점수를 계산")
  void process_minRating() throws Exception {
    // given
    PopularBookRawData rawData = new PopularBookRawData(
        UUID.randomUUID(),
        5,
        1.0
    );

    // when
    PopularBook result = popularBookProcessor.create(Period.MONTHLY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isCloseTo(2.6, offset(0.001));
    assertThat(result.getPeriod()).isEqualTo(Period.MONTHLY);
  }

  @Test
  @DisplayName("성공: 평점이 0일 때 점수를 계산")
  void process_zeroRating() throws Exception {
    // given - DB 기본값 상태 (리뷰 없음)
    PopularBookRawData rawData = new PopularBookRawData(
        UUID.randomUUID(),
        0,
        0.0
    );

    // when
    PopularBook result = popularBookProcessor.create(Period.ALL_TIME).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isEqualTo(0.0);
    assertThat(result.getPeriod()).isEqualTo(Period.ALL_TIME);
  }

  @Test
  @DisplayName("성공: 평점이 최대값(5)일 때 점수를 계산")
  void process_maxRating() throws Exception {
    // given
    PopularBookRawData rawData = new PopularBookRawData(
        UUID.randomUUID(),
        5,
        5.0
    );

    // when
    PopularBook result = popularBookProcessor.create(Period.DAILY).process(rawData);

    // then
    assertThat(result).isNotNull();
    assertThat(result.getScore()).isEqualTo(5.0);
  }

  @Test
  @DisplayName("Period가 올바르게 설정됨")
  void process_periodIsSet() throws Exception {
    // given
    PopularBookRawData rawData = new PopularBookRawData(
        UUID.randomUUID(),
        1,
        3.0
    );

    // when & then
    for (Period period : Period.values()) {
      PopularBook result = popularBookProcessor.create(period).process(rawData);
      assertThat(result).isNotNull();
      assertThat(result.getPeriod()).isEqualTo(period);
    }
  }


}
