package com.team3.deokhugam.batch.step.listener;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PopularBookRankingPersistenceService;
import com.team3.deokhugam.batch.step.writer.PopularBookWriter;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
public class PopularBookRankingListenerTest {

  @Mock
  private PopularBookRankingPersistenceService persistenceService;

  private PopularBookWriter popularBookWriter;

  @Captor
  private ArgumentCaptor<List<PopularBook>> captor;

  @BeforeEach
  void setUp() {
    popularBookWriter = new PopularBookWriter();
    popularBookWriter.beforeStep(mock(StepExecution.class));
  }

  @Test
  @DisplayName("성공: afterStep에서 전체 기준으로 순위 부여")
  void afterStep_assignsGlobalRank() throws Exception{
    popularBookWriter.create().write(new Chunk<>(List.of(
        createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(5.0)),
        createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(5.0)),
        createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(1.0))
    )));

    StepExecution stepExecution = mock(StepExecution.class);
    when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

    PopularBookRankingListener listener = new PopularBookRankingListener(
        Period.DAILY, popularBookWriter, persistenceService);
    listener.afterStep(stepExecution);

    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());

    List<PopularBook> saved = captor.getValue();
    assertThat(saved.get(0).getRank()).isEqualTo(1);
    assertThat(saved.get(1).getRank()).isEqualTo(1); // 동점
    assertThat(saved.get(2).getRank()).isEqualTo(3); // 2위 건너뜀
  }

  @Test
  @DisplayName("성공: 데이터가 없을 때 빈 리스트로 저장 호출")
  void afterStep_doesNotSaveWhenEmpty() {
    StepExecution stepExecution = mock(StepExecution.class);
    when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

    PopularBookRankingListener listener = new PopularBookRankingListener(
        Period.DAILY, popularBookWriter, persistenceService);
    listener.afterStep(stepExecution);

    verify(persistenceService).deleteAndSave(eq(Period.DAILY), eq(List.of()));
  }

  @Test
  @DisplayName("성공: 500건 초과 전체 기준으로 순위 부여")
  void afterStep_assignsGlobalRank_forLargeDataset() throws Exception{
    // 데이터 550건
    List<PopularBook> books = new ArrayList<>();
    for (int i = 550; i >= 1; i--) {
      books.add(createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(i)));
    }
    popularBookWriter.create().write(new Chunk<>(books));

    StepExecution stepExecution = mock(StepExecution.class);
    when(stepExecution.getStatus()).thenReturn(BatchStatus.COMPLETED);
    when(stepExecution.getExitStatus()).thenReturn(ExitStatus.COMPLETED);

    PopularBookRankingListener listener = new PopularBookRankingListener(
        Period.DAILY, popularBookWriter, persistenceService);
    listener.afterStep(stepExecution);

    inOrder(persistenceService);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());

    List<PopularBook> saved = captor.getValue();

    assertThat(saved).hasSize(550);
    assertThat(saved.get(0).getRank()).isEqualTo(1);
    assertThat(saved.get(499).getRank()).isEqualTo(500);
    assertThat(saved.get(500).getRank()).isEqualTo(501);
    assertThat(saved.get(549).getRank()).isEqualTo(550);
  }

  @Test
  @DisplayName("실패: 스텝 실패 시 랭킹 계산 및 저장 수행하지 않음")
  void afterStep_doesNothing_whenStepFailed() {
    StepExecution stepExecution = mock(StepExecution.class);
    when(stepExecution.getStatus()).thenReturn(BatchStatus.FAILED);
    when(stepExecution.getExitStatus()).thenReturn(ExitStatus.FAILED);

    PopularBookRankingListener listener =
        new PopularBookRankingListener(Period.DAILY, popularBookWriter, persistenceService);
    listener.afterStep(stepExecution);

    verifyNoInteractions(persistenceService);
  }

  @Test
  @DisplayName("실패: stepExecution이 null이면 FAILED 반환")
  void afterStep_returnsFailed_whenStepExecutionIsNull() {
    PopularBookRankingListener listener =
        new PopularBookRankingListener(Period.DAILY, popularBookWriter, persistenceService);
    ExitStatus result = listener.afterStep(null);

    assertThat(result).isEqualTo(ExitStatus.FAILED);
    verifyNoInteractions(persistenceService);
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
