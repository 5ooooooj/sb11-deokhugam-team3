package com.team3.deokhugam.batch.job;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PopularReviewRankingPersistenceService;
import com.team3.deokhugam.batch.step.listener.PopularReviewRankingListener;
import com.team3.deokhugam.batch.step.processor.PopularReviewProcessor;
import com.team3.deokhugam.batch.step.reader.PopularReviewReader;
import com.team3.deokhugam.batch.step.writer.PopularReviewWriter;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PopularReviewJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final PopularReviewReader popularReviewReader;
  private final PopularReviewRankingPersistenceService persistenceService;

  @Value("${batch.popular-review.chunk-size:500}")
  private int chunkSize;

  @Bean
  public Job popularReviewJob() {
    return new JobBuilder("popularReviewJob", jobRepository)
        .start(popularReviewStep(Period.DAILY))
        .next(popularReviewStep(Period.WEEKLY))
        .next(popularReviewStep(Period.MONTHLY))
        .next(popularReviewStep(Period.ALL_TIME))
        .build();
  }

  private Step popularReviewStep(Period period) {
    PopularReviewWriter writer = popularReviewWriter();
    PopularReviewProcessor processor = popularReviewProcessor();

    return new StepBuilder("popularReviewStep_" + period.name(), jobRepository)
        .<PopularReviewRawData, PopularReview>chunk(chunkSize, transactionManager)
        .reader(popularReviewReader.create(period))
        .processor(processor.create(period))
        .listener(processor)
        .writer(writer.create())
        .listener(writer)
        .listener(new PopularReviewRankingListener(period, writer, persistenceService))
        .build();
  }

  private PopularReviewWriter popularReviewWriter() {
    return new PopularReviewWriter();
  }

  private PopularReviewProcessor popularReviewProcessor() {
    return new PopularReviewProcessor();
  }
}
