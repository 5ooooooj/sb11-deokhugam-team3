package com.team3.deokhugam.repository.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface PowerUserRepository extends JpaRepository<PowerUser, UUID> {

  List<PowerUser> findByPeriodOrderByScoreDesc(Period period);

  @Modifying
  @Query("DELETE FROM PowerUser p WHERE p.period = :period")
  void deleteByPeriod(Period period);

}
