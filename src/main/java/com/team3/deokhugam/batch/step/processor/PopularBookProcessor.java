package com.team3.deokhugam.batch.step.processor;

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

@Component
public class PopularBookProcessor implements StepExecutionListener {

  private Instant calculatedAt;

  @Override
  public void beforeStep(@Nullable StepExecution stepExecution) {
    calculatedAt = (stepExecution != null && stepExecution.getStartTime() != null)
        ? stepExecution.getStartTime().toInstant(ZoneOffset.UTC)
        : Instant.now();
  }

  public ItemProcessor<PopularBookRawData, PopularBook> create(Period period) {
    return item -> {
      BigDecimal score = BigDecimal.valueOf(item.reviewCount()).multiply(BigDecimal.valueOf(0.4))
          .add(item.ratingAvg().multiply(BigDecimal.valueOf(0.6)));
      return PopularBook.builder()
          .bookId(item.bookId())
          .period(period)
          .score(score)
          .reviewCount(item.reviewCount())
          .rating(item.ratingAvg())
          .calculatedAt(calculatedAt)
          .build();
    };
  }


}
