package com.team3.deokhugam.batch.persistenceService;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import com.team3.deokhugam.repository.dashboard.PowerUserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Service
@RequiredArgsConstructor
public class PowerUserRankingPersistenceService {

  private final PowerUserRepository powerUserRepository;
  private final TransactionTemplate transactionTemplate;

  @Retryable(
      retryFor = {TransientDataAccessException.class, CannotAcquireLockException.class},
      backoff = @Backoff(delay = 2000)
  )
  public void deleteAndSave(Period period, List<PowerUser> all) {
    transactionTemplate.execute(status -> {
      powerUserRepository.deleteByPeriod(period);
      powerUserRepository.saveAll(all);
      return null;
    });
  }
}
