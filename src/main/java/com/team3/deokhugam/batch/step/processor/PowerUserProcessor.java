package com.team3.deokhugam.batch.step.processor;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import java.math.BigDecimal;
import java.time.Instant;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.stereotype.Component;

@Component
public class PowerUserProcessor {

  public ItemProcessor<PowerUserRawData, PowerUser> create(Period period) {
    return item -> {
      BigDecimal score = item.reviewScoreSum().multiply(BigDecimal.valueOf(0.5))
          .add(BigDecimal.valueOf(item.likeCount() * 0.2))
          .add(BigDecimal.valueOf(item.commentCount() * 0.3));

      return PowerUser.builder()
          .userId(item.userId())
          .period(period)
          .score(score)
          .rank(0)
          .reviewScoreSum(item.reviewScoreSum())
          .likeCount(item.likeCount())
          .commentCount(item.commentCount())
          .calculatedAt(Instant.now())
          .build();
    };
  }
}
