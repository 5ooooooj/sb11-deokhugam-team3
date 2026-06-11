package com.team3.deokhugam.batch.job;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PopularReviewRankingPersistenceService;
import com.team3.deokhugam.batch.step.reader.PopularReviewReader;
import com.team3.deokhugam.batch.step.writer.PopularReviewWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
@RequiredArgsConstructor
public class PopularReviewJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final PopularReviewReader popularReviewReader;
  private final PopularReviewRankingPersistenceService persistenceService;

  @Bean
  public Job popularReviewDailyJob() {
    return buildJob(Period.DAILY);
  }

  @Bean
  public Job popularReviewWeeklyJob() {
    return buildJob(Period.WEEKLY);
  }

  @Bean
  public Job popularReviewMonthlyJob() {
    return buildJob(Period.MONTHLY);
  }

  @Bean
  public Job popularReviewAllTimeJob() {
    return buildJob(Period.ALL_TIME);
  }

  private Job buildJob(Period period) {
    return new JobBuilder("popularReviewJob_" + period.name(), jobRepository)
        .start(popularReviewStep(period))
        .build();
  }

  private Step popularReviewStep(Period period) {
    PopularReviewWriter writer = new PopularReviewWriter(period, persistenceService);

    return new StepBuilder("popularReviewStep_" + period.name(), jobRepository)
        .<PopularReviewRawData, PopularReviewRawData>chunk(100, transactionManager)
        .reader(popularReviewReader.create(period))
        .writer(writer.create())
        .listener(writer)
        .build();
  }
}
