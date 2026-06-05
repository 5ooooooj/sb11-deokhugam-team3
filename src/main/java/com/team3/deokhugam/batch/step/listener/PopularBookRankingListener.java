package com.team3.deokhugam.batch.step.listener;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.global.RankCalculateUtil;
import com.team3.deokhugam.batch.persistenceService.PopularBookRankingPersistenceService;
import com.team3.deokhugam.batch.step.writer.PopularBookWriter;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.Nullable;

@Slf4j
@RequiredArgsConstructor
public class PopularBookRankingListener implements StepExecutionListener {

  private final Period period;
  private final PopularBookWriter popularBookWriter;
  private final PopularBookRankingPersistenceService persistenceService;

  @Override
  public ExitStatus afterStep(@Nullable StepExecution stepExecution) {

    if (stepExecution == null || stepExecution.getStatus() != BatchStatus.COMPLETED) {
      return stepExecution != null ? stepExecution.getExitStatus() : ExitStatus.FAILED;
    }

    try {
      System.out.println("=== afterStep 시작 period=" + period);
      List<PopularBook> all = popularBookWriter.getAccumulated();
      System.out.println("=== accumulated size=" + all.size());
      log.info("accumulated size: {}", all.size()); // 추가
      all.sort(Comparator.comparing(PopularBook::getScore).reversed());
      RankCalculateUtil.assignRanks(all, PopularBook::getScore, PopularBook::assignRank);
      persistenceService.deleteAndSave(period, all);
      log.info("deleteAndSave 완료 period={}", period); // 추가
    } catch (Exception e) {
      log.error("RankingListener afterStep 실패", e);
      return ExitStatus.FAILED;
    }
    return stepExecution.getExitStatus();
  }

}
