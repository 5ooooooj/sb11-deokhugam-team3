package com.team3.deokhugam.batch.step.writer;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.global.RankCalculateUtil;
import com.team3.deokhugam.batch.persistenceService.PopularReviewRankingPersistenceService;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.time.Instant;
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
public class PopularReviewWriter implements StepExecutionListener {

  private final Period period;
  private final PopularReviewRankingPersistenceService persistenceService;
  private Instant calculatedAt;

  @Override
  public void beforeStep(@Nullable StepExecution stepExecution) {
    this.calculatedAt = Instant.now();
  }

  @Override
  public ExitStatus afterStep(@Nullable StepExecution stepExecution) {
    if (stepExecution != null && stepExecution.getReadCount() == 0L) {
      persistenceService.deleteAndSave(period, List.of());
    }
    return null;
  }

  public ItemWriter<PopularReviewRawData> create() {
    return chunk -> {
      try {
        List<PopularReview> items = chunk.getItems().stream()
            .map(item -> PopularReview.builder()
                .reviewId(item.reviewId())
                .period(period)
                .score(item.score())
                .likeCount(item.likeCount())
                .commentCount(item.commentCount())
                .calculatedAt(calculatedAt)
                .build())
            .collect(Collectors.toList());

        RankCalculateUtil.assignRanks(items, PopularReview::getScore, PopularReview::assignRank, true);
        persistenceService.deleteAndSave(period, items);
        log.info("PopularReviewWriter 저장 완료 period={}, size={}", period, items.size());
      } catch (Exception e) {
        log.error("PopularReviewWriter 저장 실패 period={}", period, e);
        throw e;
      }
    };
  }
}