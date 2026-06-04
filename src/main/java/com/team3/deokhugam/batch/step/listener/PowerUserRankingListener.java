package com.team3.deokhugam.batch.step.listener;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.global.RankCalculateUtil;
import com.team3.deokhugam.batch.persistenceService.PowerUserRankingPersistenceService;
import com.team3.deokhugam.batch.step.writer.PowerUserWriter;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.lang.Nullable;

@RequiredArgsConstructor
public class PowerUserRankingListener implements StepExecutionListener {

  private final PowerUserRankingPersistenceService persistenceService;
  private final Period period;
  private final PowerUserWriter powerUserWriter;

  @Override
  public ExitStatus afterStep(@Nullable StepExecution stepExecution) {
    if (stepExecution == null || stepExecution.getStatus() != BatchStatus.COMPLETED) {
      return stepExecution != null ? stepExecution.getExitStatus() : ExitStatus.FAILED;
    }

    List<PowerUser> all = powerUserWriter.getAccumulated();

    all.sort(Comparator.comparing(PowerUser::getScore).reversed());

    RankCalculateUtil.assignRanks(all, PowerUser::getScore, PowerUser::assignRank);

    persistenceService.deleteAndSave(period, all);

    return stepExecution.getExitStatus();
  }
}
