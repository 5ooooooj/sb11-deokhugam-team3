package com.team3.deokhugam.batch.job;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PowerUserRankingPersistenceService;
import com.team3.deokhugam.batch.step.reader.PowerUserReader;
import com.team3.deokhugam.batch.step.writer.PowerUserWriter;
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
public class PowerUserJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final PowerUserRankingPersistenceService persistenceService;
  private final PowerUserReader powerUserReader;

  @Bean
  public Job powerUserDailyJob() {
    return buildJob(Period.DAILY);
  }

  @Bean
  public Job powerUserWeeklyJob() {
    return buildJob(Period.WEEKLY);
  }

  @Bean
  public Job powerUserMonthlyJob() {
    return buildJob(Period.MONTHLY);
  }

  @Bean
  public Job powerUserAllTimeJob() {
    return buildJob(Period.ALL_TIME);
  }

  private Job buildJob(Period period) {
    return new JobBuilder("powerUserJob_" + period.name(), jobRepository)
        .start(powerUserStep(period))
        .build();
  }

  private Step powerUserStep(Period period) {
    PowerUserWriter writer = new PowerUserWriter(period, persistenceService);

    return new StepBuilder("powerUserStep_" + period.name(), jobRepository)
        .<PowerUserRawData, PowerUserRawData>chunk(100, transactionManager)
        .reader(powerUserReader.create(period))
        .writer(writer.create())
        .listener(writer)
        .build();
  }
}
