package com.team3.deokhugam.batch.step.listener;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

@ExtendWith(MockitoExtension.class)
public class PopularBookStepListenerTest {

  @Mock
  private PopularBookRepository popularBookRepository;

  @Mock
  private PlatformTransactionManager transactionManager;

  @Test
  @DisplayName("성공: beforeStep에서 해당 period 데이터를 삭제")
  void beforeStep_deletesByPeriod() {
    TransactionStatus status = mock(TransactionStatus.class);
    given(transactionManager.getTransaction(any()))
        .willReturn(status);

    PopularBookStepListener listener =
        new PopularBookStepListener(popularBookRepository, transactionManager, Period.DAILY);

    listener.beforeStep(mock(StepExecution.class));

    verify(popularBookRepository, times(1)).deleteByPeriod(Period.DAILY);
    verify(transactionManager).commit(status);
  }

  @Test
  @DisplayName("성공: 다른 period는 삭제하지 않음")
  void beforeStep_deletesOnlyTargetPeriod() {
    given(transactionManager.getTransaction(any()))
        .willReturn(mock(TransactionStatus.class));

    PopularBookStepListener listener =
        new PopularBookStepListener(popularBookRepository, transactionManager, Period.WEEKLY);

    listener.beforeStep(mock(StepExecution.class));

    verify(popularBookRepository).deleteByPeriod(Period.WEEKLY);
    verify(popularBookRepository, never()).deleteByPeriod(Period.DAILY);
    verify(popularBookRepository, never()).deleteByPeriod(Period.MONTHLY);
    verify(popularBookRepository, never()).deleteByPeriod(Period.ALL_TIME);
  }
}
