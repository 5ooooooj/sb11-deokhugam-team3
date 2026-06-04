package com.team3.deokhugam.batch.job;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.step.listener.PowerUserRankingListener;
import com.team3.deokhugam.batch.step.processor.PowerUserProcessor;
import com.team3.deokhugam.batch.step.reader.PowerUserReader;
import com.team3.deokhugam.batch.step.writer.PowerUserWriter;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import com.team3.deokhugam.repository.dashboard.PowerUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Configuration
@RequiredArgsConstructor
public class PowerUserJobConfig {

  private final JobRepository jobRepository;
  private final PlatformTransactionManager transactionManager;
  private final TransactionTemplate transactionTemplate;
  private final PowerUserReader powerUserReader;
  private final PowerUserProcessor powerUserProcessor;
  private final PowerUserWriter powerUserWriter;
  private final PowerUserRepository powerUserRepository;

  public Job powerUserJob() {
    return new JobBuilder("powerUserJob", jobRepository)
        .start(powerUserStep(Period.DAILY))
        .next(powerUserStep(Period.WEEKLY))
        .next(powerUserStep(Period.MONTHLY))
        .next(powerUserStep(Period.ALL_TIME))
        .build();
  }

  public Step powerUserStep(Period period) {
    return new StepBuilder("powerUserStep_" + period, jobRepository)
        .<PowerUserRawData, PowerUser>chunk(500, transactionManager)
        .reader(powerUserReader.create(period))
        .processor(powerUserProcessor.create(period))
        .writer(powerUserWriter.create())
        .listener(powerUserWriter)
        .listener(new PowerUserRankingListener(
            powerUserRepository, transactionTemplate, period, powerUserWriter))
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
