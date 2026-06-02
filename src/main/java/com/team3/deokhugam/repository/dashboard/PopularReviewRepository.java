package com.team3.deokhugam.repository.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PopularReviewRepository extends JpaRepository<PopularReview, UUID> {
  void deleteByPeriod(Period period);
}
