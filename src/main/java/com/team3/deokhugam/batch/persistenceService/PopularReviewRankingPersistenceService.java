package com.team3.deokhugam.batch.persistenceService;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import com.team3.deokhugam.repository.dashboard.PopularReviewRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PopularReviewRankingPersistenceService {

  private final PopularReviewRepository popularReviewRepository;

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  @Retryable(
      retryFor = {TransientDataAccessException.class, CannotAcquireLockException.class},
      backoff = @Backoff(delay = 2000)
  )
  public void deleteAndSave(Period period, List<PopularReview> all) {
      popularReviewRepository.deleteByPeriod(period);
      popularReviewRepository.saveAll(all);
  }
}
