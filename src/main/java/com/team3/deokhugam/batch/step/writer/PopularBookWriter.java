package com.team3.deokhugam.batch.step.writer;

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.global.RankCalculateUtil;
import com.team3.deokhugam.batch.persistenceService.PopularBookRankingPersistenceService;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import java.time.Instant;
import java.time.ZoneId;
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
public class PopularBookWriter implements StepExecutionListener {

  private final Period period;
  private final PopularBookRankingPersistenceService persistenceService;
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

  public ItemWriter<PopularBookRawData> create() {
    return chunk -> {
      try {
        List<PopularBook> items = chunk.getItems().stream()
            .map(item -> PopularBook.builder()
                .bookId(item.bookId())
                .period(period)
                .score(item.score())
                .reviewCount(item.reviewCount())
                .rating(item.ratingAvg())
                .calculatedAt(calculatedAt)
                .build())
            .collect(Collectors.toList());

        RankCalculateUtil.assignRanks(items, PopularBook::getScore, PopularBook::assignRank, true);
        persistenceService.deleteAndSave(period, items);
        log.info("PopularBookWriter 저장 완료 period={}, size={}", period, items.size());
      } catch (Exception e) {
        log.error("PopularBookWriter 저장 실패 period={}", period, e);
        throw e;
      }
    };
  }
}
