package com.team3.deokhugam.repository.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import com.team3.deokhugam.dto.dashboard.PowerUserDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface PowerUserRepository extends JpaRepository<PowerUser, UUID> {

  @Query("""
        SELECT new com.team3.deokhugam.dto.dashboard.PowerUserDto(
            pu.userId, u.nickname,
            pu.period, pu.calculatedAt, pu.ranking, pu.score,
            pu.reviewScoreSum, pu.likeCount, pu.commentCount
        )
        FROM PowerUser pu
        JOIN User u ON pu.userId = u.id
        WHERE pu.period = :period
        ORDER BY pu.ranking ASC, u.createdAt ASC, u.id ASC
        """)
  List<PowerUserDto> findPowerUsersByPeriod(
      @Param("period") Period period,
      Pageable pageable
  );

  int countByPeriod(Period period);

  List<PowerUser> findByPeriod(Period period);

  List<PowerUser> findByPeriodOrderByScoreDesc(Period period);

  @Modifying
  @Query("DELETE FROM PowerUser p WHERE p.period = :period")
  void deleteByPeriod(Period period);

}
