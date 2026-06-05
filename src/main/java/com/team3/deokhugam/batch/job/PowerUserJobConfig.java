package com.team3.deokhugam.batch.job;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.persistenceService.PowerUserRankingPersistenceService;
import com.team3.deokhugam.batch.step.listener.PowerUserRankingListener;
import com.team3.deokhugam.batch.step.processor.PowerUserProcessor;
import com.team3.deokhugam.batch.step.reader.PowerUserReader;
import com.team3.deokhugam.batch.step.writer.PowerUserWriter;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
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

  @Value("${batch.popular-review.chunk-size:500}")
  private int chunkSize;

  @Bean
  public Job powerUserJob() {
    return new JobBuilder("powerUserJob", jobRepository)
        .start(powerUserStep(Period.DAILY))
        .next(powerUserStep(Period.WEEKLY))
        .next(powerUserStep(Period.MONTHLY))
        .next(powerUserStep(Period.ALL_TIME))
        .build();
  }

  public Step powerUserStep(Period period) {
    PowerUserProcessor processor = powerUserProcessor();
    PowerUserWriter writer = powerUserWriter();

    return new StepBuilder("powerUserStep_" + period, jobRepository)
        .<PowerUserRawData, PowerUser>chunk(chunkSize, transactionManager)
        .reader(powerUserReader.create(period))
        .processor(processor.create(period))
        .listener(processor)
        .writer(writer.create())
        .listener(writer)
        .listener(new PowerUserRankingListener(
            persistenceService, period, writer))
        .build();
  }

  private PowerUserWriter powerUserWriter() {
    return new PowerUserWriter();
  }

  private PowerUserProcessor powerUserProcessor() {
    return new PowerUserProcessor();
  }

}
