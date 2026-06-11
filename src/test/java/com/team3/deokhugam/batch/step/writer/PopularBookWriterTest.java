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

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PopularBookRankingPersistenceService;
import com.team3.deokhugam.domain.dashboard.PopularBook;
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
  @DisplayName("성공: 0건일 때 afterStep에서 deleteAndSave 호출됨")
  void afterStep_noItems_deleteAndSaveCalled() {
    StepExecution stepExecution = mock(StepExecution.class);
    doReturn(0L).when(stepExecution).getReadCount();

    popularBookWriter.afterStep(stepExecution);

    verify(persistenceService).deleteAndSave(eq(Period.DAILY), eq(List.of()));
  }

  @Test
  @DisplayName("성공: readCount > 0이면 afterStep에서 deleteAndSave 호출 안 됨")
  void afterStep_hasItems_deleteAndSaveNotCalled() {
    StepExecution stepExecution = mock(StepExecution.class);
    doReturn(1L).when(stepExecution).getReadCount();

    popularBookWriter.afterStep(stepExecution);

    verify(persistenceService, never()).deleteAndSave(any(), any());
  }

  @Test
  @DisplayName("성공: afterStep에 null 전달 시 예외 없이 처리됨")
  void afterStep_null_noException() {
    assertThatNoException().isThrownBy(() -> popularBookWriter.afterStep(null));
  }

  @Test
  @DisplayName("성공: beforeStep에 null 전달 시 Instant.now()로 설정됨")
  void beforeStep_null_usesInstantNow() throws Exception {
    PopularBookWriter writer = new PopularBookWriter(Period.DAILY, persistenceService);
    writer.beforeStep(null);

    List<PopularBookRawData> items = List.of(
        new PopularBookRawData(UUID.randomUUID(), 1, BigDecimal.valueOf(5.0), BigDecimal.valueOf(5.0))
    );
    writer.create().write(new Chunk<>(items));

    ArgumentCaptor<List<PopularBook>> captor = ArgumentCaptor.forClass(List.class);
    verify(persistenceService).deleteAndSave(eq(Period.DAILY), captor.capture());
    assertThat(captor.getValue().get(0).getCalculatedAt()).isNotNull();
  }
}
