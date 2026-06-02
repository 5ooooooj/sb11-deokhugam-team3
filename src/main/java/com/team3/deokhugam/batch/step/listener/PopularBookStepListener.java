package com.team3.deokhugam.batch.step.listener;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Component
@RequiredArgsConstructor
public class PopularBookStepListener implements StepExecutionListener {

  private final PopularBookRepository popularBookRepository;
  private final PlatformTransactionManager transactionManager;
  private Period period;

  public PopularBookStepListener forPeriod(Period period) {
    this.period = period;
    return this;
  }

  @Override
  public void beforeStep(@Nullable StepExecution stepExecution) {
    TransactionTemplate transactionTemplate =
        new TransactionTemplate(transactionManager);
    transactionTemplate.execute(status -> {
      popularBookRepository.deleteByPeriod(period);
      return null;
    });
  }

}
