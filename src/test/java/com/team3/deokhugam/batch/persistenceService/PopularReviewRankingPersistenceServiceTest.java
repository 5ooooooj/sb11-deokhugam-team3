package com.team3.deokhugam.batch.persistenceService;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import com.team3.deokhugam.repository.dashboard.PopularReviewRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
@ActiveProfiles("test")
class PopularReviewRankingPersistenceServiceTest {

  @Autowired
  private PopularReviewRankingPersistenceService persistenceService;

  @MockitoBean
  private PopularReviewRepository popularReviewRepository;

  @Test
  @DisplayName("성공: TransientDataAccessException 발생 시 재시도 후 성공")
  void deleteAndSave_retryOnTransientException() {
    List<PopularReview> all = List.of(
        PopularReview.builder()
            .reviewId(UUID.randomUUID())
            .period(Period.DAILY)
            .score(BigDecimal.valueOf(3.0))
            .ranking(1)
            .likeCount(1)
            .commentCount(2)
            .calculatedAt(Instant.now())
            .build()
    );

    doThrow(new TransientDataAccessException("일시적 오류") {})
        .doNothing()
        .when(popularReviewRepository).deleteByPeriod(any());

    assertThatNoException()
        .isThrownBy(() -> persistenceService.deleteAndSave(Period.DAILY, all));

    verify(popularReviewRepository, times(2)).deleteByPeriod(Period.DAILY);
    verify(popularReviewRepository, times(1)).saveAll(all);
  }

  @Test
  @DisplayName("실패: 재시도 횟수 초과 시 예외 발생")
  void deleteAndSave_exhaustRetry() {
    List<PopularReview> all = List.of();

    doThrow(new TransientDataAccessException("일시적 오류") {})
        .when(popularReviewRepository).deleteByPeriod(any());

    assertThatThrownBy(() -> persistenceService.deleteAndSave(Period.DAILY, all))
        .isInstanceOf(TransientDataAccessException.class);

    verify(popularReviewRepository, times(3)).deleteByPeriod(Period.DAILY);
    verify(popularReviewRepository, never()).saveAll(any());
  }
}