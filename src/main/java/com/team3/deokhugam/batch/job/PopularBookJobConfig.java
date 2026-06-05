package com.team3.deokhugam.batch.job;

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PopularBookRankingPersistenceService;
import com.team3.deokhugam.batch.step.listener.PopularBookRankingListener;
import com.team3.deokhugam.batch.step.processor.PopularBookProcessor;
import com.team3.deokhugam.batch.step.reader.PopularBookReader;
import com.team3.deokhugam.batch.step.writer.PopularBookWriter;
import com.team3.deokhugam.domain.dashboard.PopularBook;
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
  private final PopularBookRankingPersistenceService persistenceService;

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
    PopularBookWriter writer = new PopularBookWriter();
    PopularBookProcessor processor = new PopularBookProcessor();

    return new StepBuilder("popularBookStep_" + period.name(), jobRepository)
        .<PopularBookRawData, PopularBook>chunk(500, transactionManager)
        .reader(popularBookReader.create(period))
        .processor(processor.create(period))
        .listener(processor)
        .writer(writer.create())
        .listener(writer)
        .listener(new PopularBookRankingListener(period, writer, persistenceService))
        .faultTolerant()
        .retryLimit(3)
        .retry(TransientDataAccessException.class) // 일시적 db 오류
        .retry(CannotAcquireLockException.class) // DB 락 경합
        .backOffPolicy(createBackOffPolicy())
        .build();
  }

  private FixedBackOffPolicy createBackOffPolicy() {
    FixedBackOffPolicy policy = new FixedBackOffPolicy();
    policy.setBackOffPeriod(2000L);
    return policy;
  }
}
