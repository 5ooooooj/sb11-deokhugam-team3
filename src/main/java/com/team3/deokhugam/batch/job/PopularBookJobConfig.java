package com.team3.deokhugam.batch.job;

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.step.listener.PopularBookRankingListener;
import com.team3.deokhugam.batch.step.listener.PopularBookStepListener;
import com.team3.deokhugam.batch.step.processor.PopularBookProcessor;
import com.team3.deokhugam.batch.step.reader.PopularBookReader;
import com.team3.deokhugam.batch.step.writer.PopularBookWriter;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PopularBookJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final PopularBookReader popularBookReader;
  private final PopularBookProcessor popularBookProcessor;
  private final PopularBookWriter popularBookWriter;
  private final PopularBookRepository popularBookRepository;

  @Bean
  public Job popularBookJob() {
    return new JobBuilder("popularBookJob", jobRepository)
        .start(popularBookStep(Period.DAILY))
        .next(popularBookStep(Period.WEEKLY))
        .next(popularBookStep(Period.MONTHLY))
        .next(popularBookStep(Period.ALL_TIME))
        .build();
  }

  private Step popularBookStep(Period period) {
    return new StepBuilder("popularBookStep_" + period.name(), jobRepository)
        .<PopularBookRawData, PopularBook>chunk(500, transactionManager)
        .reader(popularBookReader.create(period))
        .processor(popularBookProcessor.create(period))
        .writer(popularBookWriter.create(period))
        .listener(new PopularBookStepListener(popularBookRepository, transactionManager, period))
        .listener(new PopularBookRankingListener(popularBookRepository, period))
        .faultTolerant()
        .retryLimit(3)
        .retry(TransientDataAccessException.class) // 일시적 db 오류
        .retry(CannotAcquireLockException.class) // DB 락 경합
        .backOffPolicy(new FixedBackOffPolicy() {{
          setBackOffPeriod(2000L);
        }})
        .build();
  }
}
