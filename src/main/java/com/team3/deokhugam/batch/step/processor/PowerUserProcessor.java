package com.team3.deokhugam.batch.step.processor;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PowerUserProcessor implements StepExecutionListener {

  private Instant calculatedAt;

  @Override
  public void beforeStep(@Nullable StepExecution stepExecution) {
    calculatedAt = (stepExecution != null && stepExecution.getStartTime() != null)
        ? stepExecution.getStartTime().toInstant(ZoneOffset.UTC)
        : Instant.now();
  }

  public ItemProcessor<PowerUserRawData, PowerUser> create(Period period) {
    return item -> {
      BigDecimal score = item.reviewScoreSum().multiply(BigDecimal.valueOf(0.5))
          .add(BigDecimal.valueOf(item.likeCount()).multiply(BigDecimal.valueOf(0.2)))
          .add(BigDecimal.valueOf(item.commentCount()).multiply(BigDecimal.valueOf(0.3)));

      return PowerUser.builder()
          .userId(item.userId())
          .period(period)
          .score(score)
          .rank(0)
          .reviewScoreSum(item.reviewScoreSum())
          .likeCount(item.likeCount())
          .commentCount(item.commentCount())
          .calculatedAt(calculatedAt)
          .build();
    };
  }
}
