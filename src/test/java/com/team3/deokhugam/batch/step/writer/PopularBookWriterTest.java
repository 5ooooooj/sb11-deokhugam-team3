package com.team3.deokhugam.batch.step.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PopularBookRankingPersistenceService;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
class PopularBookWriterTest {

  @Mock
  private PopularBookRankingPersistenceService persistenceService;

  private PopularBookWriter popularBookWriter;

  @BeforeEach
  void setUp() {
    popularBookWriter = new PopularBookWriter(Period.DAILY, persistenceService);
    StepExecution stepExecution = mock(StepExecution.class);
    when(stepExecution.getStartTime()).thenReturn(LocalDateTime.now());
    popularBookWriter.beforeStep(stepExecution);
  }

  @Test
  @DisplayName("성공: RawData를 PopularBook으로 변환 후 저장")
  void write_success() throws Exception {
    List<PopularBookRawData> items = List.of(
        new PopularBookRawData(UUID.randomUUID(), 5, BigDecimal.valueOf(4.0), BigDecimal.valueOf(4.4)),
        new PopularBookRawData(UUID.randomUUID(), 3, BigDecimal.valueOf(3.0), BigDecimal.valueOf(3.0))
    );

    popularBookWriter.create().write(new Chunk<>(items));

    ArgumentCaptor<List<PopularBook>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());

    List<PopularBook> saved = captor.getValue();
    assertThat(saved).hasSize(2);
    assertThat(saved.get(0).getScore()).isEqualByComparingTo(BigDecimal.valueOf(4.4));
    assertThat(saved.get(1).getScore()).isEqualByComparingTo(BigDecimal.valueOf(3.0));
  }

  @Test
  @DisplayName("성공: rank가 순서대로 부여됨")
  void write_rankAssigned() throws Exception {
    List<PopularBookRawData> items = List.of(
        new PopularBookRawData(UUID.randomUUID(), 5, BigDecimal.valueOf(5.0), BigDecimal.valueOf(5.0)),
        new PopularBookRawData(UUID.randomUUID(), 3, BigDecimal.valueOf(3.0), BigDecimal.valueOf(3.0)),
        new PopularBookRawData(UUID.randomUUID(), 1, BigDecimal.valueOf(1.0), BigDecimal.valueOf(1.0))
    );

    popularBookWriter.create().write(new Chunk<>(items));

    ArgumentCaptor<List<PopularBook>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());

    List<PopularBook> saved = captor.getValue();
    assertThat(saved.get(0).getRanking()).isEqualTo(1);
    assertThat(saved.get(1).getRanking()).isEqualTo(2);
    assertThat(saved.get(2).getRanking()).isEqualTo(3);
  }

  @Test
  @DisplayName("성공: 동점이면 같은 rank 부여 (1, 1, 3)")
  void write_sameScore_sameRank() throws Exception {
    List<PopularBookRawData> items = List.of(
        new PopularBookRawData(UUID.randomUUID(), 3, BigDecimal.valueOf(4.0), BigDecimal.valueOf(4.0)),
        new PopularBookRawData(UUID.randomUUID(), 3, BigDecimal.valueOf(4.0), BigDecimal.valueOf(4.0)),
        new PopularBookRawData(UUID.randomUUID(), 1, BigDecimal.valueOf(2.0), BigDecimal.valueOf(2.0))
    );

    popularBookWriter.create().write(new Chunk<>(items));

    ArgumentCaptor<List<PopularBook>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());

    List<PopularBook> saved = captor.getValue();
    assertThat(saved.stream().filter(b -> b.getRanking() == 1)).hasSize(2);
    assertThat(saved.stream().filter(b -> b.getRanking() == 3)).hasSize(1);
  }

  @Test
  @DisplayName("성공: 빈 chunk도 저장 호출됨")
  void write_emptyChunk() throws Exception {
    popularBookWriter.create().write(new Chunk<>(List.of()));

    verify(persistenceService).deleteAndSave(eq(Period.DAILY), eq(List.of()));
  }

  @Test
  @DisplayName("성공: calculatedAt이 Step 시작 시간으로 설정됨")
  void write_calculatedAt_isStepStartTime() throws Exception {
    LocalDateTime stepStartTime = LocalDateTime.of(2026, 6, 10, 0, 0, 0);
    StepExecution stepExecution = mock(StepExecution.class);
    when(stepExecution.getStartTime()).thenReturn(stepStartTime);

    PopularBookWriter writer = new PopularBookWriter(Period.DAILY, persistenceService);
    writer.beforeStep(stepExecution);

    List<PopularBookRawData> items = List.of(
        new PopularBookRawData(UUID.randomUUID(), 1, BigDecimal.valueOf(5.0), BigDecimal.valueOf(5.0))
    );
    writer.create().write(new Chunk<>(items));

    ArgumentCaptor<List<PopularBook>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());

    Instant expected = stepStartTime.toInstant(ZoneOffset.UTC);
    assertThat(captor.getValue().get(0).getCalculatedAt()).isEqualTo(expected);
  }
}
