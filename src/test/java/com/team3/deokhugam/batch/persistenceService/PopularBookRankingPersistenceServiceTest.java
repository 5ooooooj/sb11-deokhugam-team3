package com.team3.deokhugam.batch.persistenceService;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
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
class PopularBookRankingPersistenceServiceTest {

  @Autowired
  private PopularBookRankingPersistenceService persistenceService;

  @MockitoBean
  private PopularBookRepository popularBookRepository;

  @Test
  @DisplayName("성공: TransientDataAccessException 발생 시 재시도 후 성공")
  void deleteAndSave_retryOnTransientException() {
    List<PopularBook> all = List.of(
        PopularBook.builder()
            .bookId(UUID.randomUUID())
            .period(Period.DAILY)
            .score(BigDecimal.valueOf(3.0))
            .ranking(1)
            .reviewCount(1)
            .rating(BigDecimal.valueOf(3.0))
            .calculatedAt(Instant.now())
            .build()
    );

    // 첫 번째 호출은 실패, 두 번째는 성공
    doThrow(new TransientDataAccessException("일시적 오류") {})
        .doNothing()
        .when(popularBookRepository).deleteByPeriod(any());

    assertThatNoException()
        .isThrownBy(() -> persistenceService.deleteAndSave(Period.DAILY, all));

    verify(popularBookRepository, times(2)).deleteByPeriod(Period.DAILY);
    verify(popularBookRepository, times(1)).saveAll(all);
  }

  @Test
  @DisplayName("실패: 재시도 횟수 초과 시 예외 발생")
  void deleteAndSave_exhaustRetry() {
    List<PopularBook> all = List.of();

    doThrow(new TransientDataAccessException("일시적 오류") {})
        .when(popularBookRepository).deleteByPeriod(any());

    assertThatThrownBy(() -> persistenceService.deleteAndSave(Period.DAILY, all))
        .isInstanceOf(TransientDataAccessException.class);

    verify(popularBookRepository, times(3)).deleteByPeriod(Period.DAILY);
    verify(popularBookRepository, never()).saveAll(any());
  }
}
