package com.team3.deokhugam.batch.step.writer;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.global.RankCalculateUtil;
import com.team3.deokhugam.batch.persistenceService.PowerUserRankingPersistenceService;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.batch.item.ItemWriter;
import org.springframework.lang.Nullable;

@Slf4j
@RequiredArgsConstructor
public class PowerUserWriter implements StepExecutionListener {

  private final Period period;
  private final PowerUserRankingPersistenceService persistenceService;
  private Instant calculatedAt;

  @Override
  public void beforeStep(@Nullable StepExecution stepExecution) {
    if (stepExecution != null && stepExecution.getStartTime() != null) {
      calculatedAt = stepExecution.getStartTime()
          .atZone(ZoneId.systemDefault())
          .toInstant();
    } else {
      calculatedAt = Instant.now();
    }
  }

  @Override
  public ExitStatus afterStep(@Nullable StepExecution stepExecution) {
    if (stepExecution != null && stepExecution.getReadCount() == 0L) {
      persistenceService.deleteAndSave(period, List.of());
    }
    return null;
  }

  public ItemWriter<PowerUserRawData> create() {
    return chunk -> {
      try {
        List<PowerUser> items = chunk.getItems().stream()
            .map(item -> PowerUser.builder()
                .userId(item.userId())
                .period(period)
                .score(item.score())
                .reviewScoreSum(item.reviewScoreSum())
                .likeCount(item.likeCount())
                .commentCount(item.commentCount())
                .calculatedAt(calculatedAt)
                .build())
            .collect(Collectors.toList());

        RankCalculateUtil.assignRanks(items, PowerUser::getScore, PowerUser::assignRank);
        persistenceService.deleteAndSave(period, items);
        log.info("PowerUserWriter 저장 완료 period={}, size={}", period, items.size());
      } catch (Exception e) {
        log.error("PowerUserWriter 저장 실패 period={}", period, e);
        throw e;
      }
    };
  }
}
