package com.team3.deokhugam.repository.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface PopularReviewRepository extends JpaRepository<PopularReview, UUID> {

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM PopularReview p WHERE p.period = :period")
  void deleteByPeriod(Period period);
}
