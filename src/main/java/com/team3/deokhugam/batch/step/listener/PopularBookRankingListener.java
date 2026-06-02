package com.team3.deokhugam.batch.step.listener;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularBookRankingListener implements StepExecutionListener {

  private final PopularBookRepository popularBookRepository;
  private Period period;

  public PopularBookRankingListener forPeriod(Period period) {
    this.period = period;
    return this;
  }

  @Override
  public ExitStatus afterStep(StepExecution stepExecution) {
    List<PopularBook> all = popularBookRepository.findByPeriodOrderByScoreDesc(period);

    int rank = 1;
    for (int i = 0; i < all.size(); i++) {
      if (i > 0 && all.get(i).getScore() == all.get(i-1).getScore()) {
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
