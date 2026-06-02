package com.team3.deokhugam.batch.step.listener;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public class PopularBookRankingListener implements StepExecutionListener {

  private final PopularBookRepository popularBookRepository;
  private final Period period;

  @Override
  public ExitStatus afterStep(@Nullable StepExecution stepExecution) {

    if (stepExecution == null || stepExecution.getStatus() != BatchStatus.COMPLETED) {
      return stepExecution != null ? stepExecution.getExitStatus() : ExitStatus.FAILED;
    }

    List<PopularBook> all = popularBookRepository.findByPeriodOrderByScoreDesc(period);

    int rank = 1;
    for (int i = 0; i < all.size(); i++) {
      if (i > 0 && all.get(i).getScore().compareTo(all.get(i-1).getScore()) == 0) {
        all.get(i).assignRank(all.get(i-1).getRank());
      } else {
        all.get(i).assignRank(rank);
      }
      rank++;
    }

    popularBookRepository.saveAll(all);
    return stepExecution.getExitStatus();
  }

}
