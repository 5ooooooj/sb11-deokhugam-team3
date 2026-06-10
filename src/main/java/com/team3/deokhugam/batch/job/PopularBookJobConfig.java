package com.team3.deokhugam.batch.job;

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PopularBookRankingPersistenceService;
import com.team3.deokhugam.batch.step.reader.PopularBookReader;
import com.team3.deokhugam.batch.step.writer.PopularBookWriter;
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
public class PopularBookJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final PopularBookReader popularBookReader;
  private final PopularBookRankingPersistenceService persistenceService;

  @Bean
  public Job popularBookDailyJob() {
    return buildJob(Period.DAILY);
  }

  @Bean public Job popularBookWeeklyJob() {
    return buildJob(Period.WEEKLY);
  }

  @Bean public Job popularBookMonthlyJob() {
    return buildJob(Period.MONTHLY);
  }

  @Bean public Job popularBookAllTimeJob() {
    return buildJob(Period.ALL_TIME);
  }

  private Job buildJob(Period period) {
    return new JobBuilder("popularBookJob_" + period.name(), jobRepository)
        .start(popularBookStep(period))
        .build();
  }

  private Step popularBookStep(Period period) {
    PopularBookWriter writer = new PopularBookWriter(period, persistenceService);

    return new StepBuilder("popularBookStep_" + period.name(), jobRepository)
        .<PopularBookRawData, PopularBookRawData>chunk(100, transactionManager)
        .reader(popularBookReader.create(period))
        .writer(writer.create())
        .listener(writer)
        .build();
  }
}
