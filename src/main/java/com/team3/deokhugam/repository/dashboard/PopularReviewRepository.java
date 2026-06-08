package com.team3.deokhugam.repository.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import com.team3.deokhugam.dto.dashboard.PopularReviewDto;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopularReviewRepository extends JpaRepository<PopularReview, UUID> {


  @Query("""
    SELECT new com.team3.deokhugam.dto.dashboard.PopularReviewDto(
        pr.id, pr.reviewId,
        r.book.id, r.book.title, r.book.thumbnailUrl,
        r.user.id, r.user.nickname,
        r.content, r.rating,
        pr.period, pr.calculatedAt, pr.rank, pr.score,
        pr.likeCount, pr.commentCount
    )
    FROM PopularReview pr
    JOIN Review r ON pr.reviewId = r.id
    WHERE pr.period = :period
    ORDER BY pr.rank ASC
    """)
  List<PopularReviewDto> findPopularReviewsByPeriod(
      @Param("period") Period period,
      Pageable pageable
  );

  int countByPeriod(Period period);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM PopularReview p WHERE p.period = :period")
  void deleteByPeriod(Period period);
}
