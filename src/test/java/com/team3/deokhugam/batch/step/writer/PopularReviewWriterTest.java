package com.team3.deokhugam.batch.step.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PopularReviewRankingPersistenceService;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.math.BigDecimal;
import java.time.LocalDateTime;
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
class PopularReviewWriterTest {

  @Mock
  private PopularReviewRankingPersistenceService persistenceService;

  private PopularReviewWriter popularReviewWriter;

  @BeforeEach
  void setUp() {
    popularReviewWriter = new PopularReviewWriter(Period.DAILY, persistenceService);
    StepExecution stepExecution = mock(StepExecution.class);
    when(stepExecution.getStartTime()).thenReturn(LocalDateTime.now());
    popularReviewWriter.beforeStep(stepExecution);
  }

  @Test
  @DisplayName("성공: 0건일 때 afterStep에서 deleteAndSave 호출됨")
  void afterStep_noItems_deleteAndSaveCalled() {
    StepExecution stepExecution = mock(StepExecution.class);
    doReturn(0L).when(stepExecution).getReadCount();

    popularReviewWriter.afterStep(stepExecution);

    verify(persistenceService).deleteAndSave(eq(Period.DAILY), eq(List.of()));
  }

  @Test
  @DisplayName("성공: readCount > 0이면 afterStep에서 deleteAndSave 호출 안 됨")
  void afterStep_hasItems_deleteAndSaveNotCalled() {
    StepExecution stepExecution = mock(StepExecution.class);
    doReturn(1L).when(stepExecution).getReadCount();

    popularReviewWriter.afterStep(stepExecution);

    verify(persistenceService, never()).deleteAndSave(any(), any());
  }

  @Test
  @DisplayName("성공: afterStep에 null 전달 시 예외 없이 처리됨")
  void afterStep_null_noException() {
    assertThatNoException().isThrownBy(() -> popularReviewWriter.afterStep(null));
  }

  @Test
  @DisplayName("성공: beforeStep에 null 전달 시 Instant.now()로 설정됨")
  void beforeStep_null_usesInstantNow() throws Exception {
    PopularReviewWriter writer = new PopularReviewWriter(Period.DAILY, persistenceService);
    writer.beforeStep(null);

    List<PopularReviewRawData> items = List.of(
        new PopularReviewRawData(UUID.randomUUID(), 3, 5, BigDecimal.valueOf(3.0 * 0.3 + 5.0 * 0.7))
    );
    writer.create().write(new Chunk<>(items));

    ArgumentCaptor<List<PopularReview>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());
    assertThat(captor.getValue().get(0).getCalculatedAt()).isNotNull();
  }

  @Test
  @DisplayName("성공: chunk 아이템이 저장되고 랭킹이 부여됨")
  void write_success() throws Exception {
    List<PopularReviewRawData> items = List.of(
        new PopularReviewRawData(UUID.randomUUID(), 1, 2, BigDecimal.valueOf(1.7)),
        new PopularReviewRawData(UUID.randomUUID(), 3, 5, BigDecimal.valueOf(4.4)),
        new PopularReviewRawData(UUID.randomUUID(), 2, 1, BigDecimal.valueOf(1.3))
    );

    popularReviewWriter.create().write(new Chunk<>(items));

    ArgumentCaptor<List<PopularReview>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());
    assertThat(captor.getValue()).hasSize(3);
    assertThat(captor.getValue()).allMatch(r -> r.getRanking() > 0);
  }

  @Test
  @DisplayName("성공: 빈 chunk 처리 시 빈 리스트로 저장됨")
  void write_emptyChunk() throws Exception {
    popularReviewWriter.create().write(new Chunk<>(List.of()));

    ArgumentCaptor<List<PopularReview>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());
    assertThat(captor.getValue()).isEmpty();
  }
}