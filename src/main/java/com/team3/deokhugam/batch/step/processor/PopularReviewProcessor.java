package com.team3.deokhugam.batch.step.processor;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PopularReviewProcessor implements StepExecutionListener {

  private Instant calculatedAt;

  @Override
  public void beforeStep(@Nullable StepExecution stepExecution) {
    calculatedAt = (stepExecution != null && stepExecution.getStartTime() != null)
        ? stepExecution.getStartTime().toInstant(ZoneOffset.UTC)
        : Instant.now();
  }

  public ItemProcessor<PopularReviewRawData, PopularReview> create(Period period) {
    return item -> {
      BigDecimal score = BigDecimal.valueOf(item.likeCount()).multiply(BigDecimal.valueOf(0.3))
          .add(BigDecimal.valueOf(item.commentCount()).multiply(BigDecimal.valueOf(0.7)));
      return PopularReview.builder()
          .reviewId(item.reviewId())
          .period(period)
          .score(score)
          .ranking(0)
          .likeCount(item.likeCount())
          .commentCount(item.commentCount())
          .calculatedAt(calculatedAt)
          .build();
    };
  }


}
