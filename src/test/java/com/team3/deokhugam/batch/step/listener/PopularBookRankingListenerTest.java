package com.team3.deokhugam.batch.step.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;

@ExtendWith(MockitoExtension.class)
public class PopularBookRankingListenerTest {

  @Mock
  private PopularBookRepository popularBookRepository;

  @Captor
  private ArgumentCaptor<List<PopularBook>> captor;

  @Test
  @DisplayName("성공: afterStep에서 전체 기준으로 순위 부여")
  void afterStep_assignsGlobalRank() {
    List<PopularBook> books = List.of(
        createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(5.0)),
        createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(5.0)),
        createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(1.0))
    );
    when(popularBookRepository.findByPeriodOrderByScoreDesc(Period.DAILY))
        .thenReturn(books);

    PopularBookRankingListener listener =
        new PopularBookRankingListener(popularBookRepository).forPeriod(Period.DAILY);
    listener.afterStep(mock(StepExecution.class));

    verify(popularBookRepository).saveAll(captor.capture());
    List<PopularBook> saved = captor.getValue();
    assertThat(saved.get(0).getRank()).isEqualTo(1);
    assertThat(saved.get(1).getRank()).isEqualTo(1); // 동점
    assertThat(saved.get(2).getRank()).isEqualTo(3); // 2위 건너뜀
  }

  @Test
  @DisplayName("성공: 결과가 없으면 저장 수행하지 않음")
  void afterStep_doesNotSaveWhenEmpty() {
    when(popularBookRepository.findByPeriodOrderByScoreDesc(Period.DAILY))
        .thenReturn(List.of());

    PopularBookRankingListener listener =
        new PopularBookRankingListener(popularBookRepository).forPeriod(Period.DAILY);
    listener.afterStep(mock(StepExecution.class));

    verify(popularBookRepository).saveAll(List.of());
  }

  @Test
  @DisplayName("성공: 500건 초과 전체 기준으로 순위 부여")
  void afterStep_assignsGlobalRank_forLargeDataset() {
    // 데이터 550건
    List<PopularBook> books = new ArrayList<>();
    for (int i = 550; i >= 1; i--) {
      books.add(createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(i)));
    }
    when(popularBookRepository.findByPeriodOrderByScoreDesc(Period.DAILY))
        .thenReturn(books);

    PopularBookRankingListener listener =
        new PopularBookRankingListener(popularBookRepository).forPeriod(Period.DAILY);
    listener.afterStep(mock(StepExecution.class));

    verify(popularBookRepository).saveAll(captor.capture());
    List<PopularBook> saved = captor.getValue();

    assertThat(saved).hasSize(550);
    assertThat(saved.get(0).getRank()).isEqualTo(1);
    assertThat(saved.get(499).getRank()).isEqualTo(500);
    assertThat(saved.get(500).getRank()).isEqualTo(501);
    assertThat(saved.get(549).getRank()).isEqualTo(550);
  }

  private PopularBook createPopularBook(UUID bookId, BigDecimal score) {
    return PopularBook.builder()
        .bookId(bookId)
        .period(Period.DAILY)
        .score(score)
        .reviewCount(0)
        .rating(BigDecimal.ZERO)
        .calculatedAt(Instant.now())
        .build();
  }
}
