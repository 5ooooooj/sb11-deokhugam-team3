package com.team3.deokhugam.batch.step.listener;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.global.RankCalculateUtil;
import com.team3.deokhugam.batch.step.writer.PopularBookWriter;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.Nullable;
import org.springframework.transaction.support.TransactionTemplate;

@RequiredArgsConstructor
public class PopularBookRankingListener implements StepExecutionListener {

  private final PopularBookRepository popularBookRepository;
  private final Period period;
  private final TransactionTemplate transactionTemplate;
  private final PopularBookWriter popularBookWriter;

  @Override
  public ExitStatus afterStep(@Nullable StepExecution stepExecution) {

    if (stepExecution == null || stepExecution.getStatus() != BatchStatus.COMPLETED) {
      return stepExecution != null ? stepExecution.getExitStatus() : ExitStatus.FAILED;
    }

    List<PopularBook> all = popularBookWriter.getAccumulated();

    all.sort(Comparator.comparing(PopularBook::getScore).reversed());

    RankCalculateUtil.assignRanks(all, PopularBook::getScore, PopularBook::assignRank);

    transactionTemplate.execute(status -> {
      popularBookRepository.deleteByPeriod(period);
      popularBookRepository.saveAll(all);
      return null;
    });

    return stepExecution.getExitStatus();
  }

}
