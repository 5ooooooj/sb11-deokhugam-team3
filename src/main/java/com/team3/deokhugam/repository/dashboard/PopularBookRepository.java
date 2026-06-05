package com.team3.deokhugam.repository.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.dto.dashboard.PopularBookDto;
import org.springframework.data.domain.Pageable;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PopularBookRepository extends JpaRepository<PopularBook, UUID> {

  @Query("""
        SELECT new com.team3.deokhugam.dto.dashboard.PopularBookDto(
            pb.id, pb.bookId,
            b.title, b.author, b.thumbnailUrl,
            pb.period, pb.rank, pb.score,
            pb.reviewCount, pb.rating, pb.calculatedAt
        )
        FROM PopularBook pb
        JOIN Book b ON pb.bookId = b.id
        WHERE pb.period = :period
        ORDER BY pb.rank ASC
        """)
  List<PopularBookDto> findPopularBookByPeriod(
      @Param("period") Period period,
      Pageable pageable
  );

  List<PopularBook> findByPeriodOrderByRankAsc(Period period, Pageable pageable);

  int countByPeriod(Period period);

  List<PopularBook> findByPeriod(Period period);

  List<PopularBook> findByPeriodOrderByScoreDesc(Period period);

  @Modifying(clearAutomatically = true)
  @Query("DELETE FROM PopularBook p WHERE p.period = :period")
  void deleteByPeriod(Period period);
}
