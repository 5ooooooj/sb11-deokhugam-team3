package com.team3.deokhugam.batch.step.reader;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.DateCalculateUtil;
import com.team3.deokhugam.batch.global.Period;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PowerUserReader {

  private final EntityManagerFactory entityManagerFactory;

  public JpaPagingItemReader<PowerUserRawData> create(Period period) {
    Instant startDate = DateCalculateUtil.getStartDate(period);

    JpaPagingItemReaderBuilder<PowerUserRawData> builder =
        new JpaPagingItemReaderBuilder<PowerUserRawData>()
            .name("powerUserReader_" + period.name())
            .entityManagerFactory(entityManagerFactory)
            .pageSize(500);

    if (startDate == null) {
      builder.queryString("""
                SELECT new com.team3.deokhugam.batch.dto.PowerUserRawData(
                    u.id,
                    CAST(COALESCE(SUM(pr.score), 0) AS bigdecimal),
                    CAST(COUNT(DISTINCT rl.id) AS int),
                    CAST(COUNT(DISTINCT c.id) AS int)
                )
                FROM User u
                LEFT JOIN Review r ON r.user.id = u.id
                LEFT JOIN PopularReview pr ON pr.reviewId = r.id
                                           AND pr.period = :period
                LEFT JOIN ReviewLike rl ON rl.user.id = u.id
                LEFT JOIN Comment c ON c.user.id = u.id
                WHERE u.deletedAt IS NULL
                GROUP BY u.id
                HAVING COALESCE(SUM(pr.score), 0) > 0
                    OR COUNT(DISTINCT rl.id) > 0
                    OR COUNT(DISTINCT c.id) > 0
                ORDER BY u.id
                """)
          .parameterValues(Map.of("period", period));
    } else {
      builder.queryString("""
                SELECT new com.team3.deokhugam.batch.dto.PowerUserRawData(
                    u.id,
                    CAST(COALESCE(SUM(pr.score), 0) AS bigdecimal ),
                    CAST(COUNT(DISTINCT rl.id) AS int),
                    CAST(COUNT(DISTINCT c.id) AS int)
                )
                FROM User u
                LEFT JOIN Review r ON r.user.id = u.id
                                   AND r.createdAt >= :startDate
                LEFT JOIN PopularReview pr ON pr.reviewId = r.id
                                           AND pr.period = :period
                LEFT JOIN ReviewLike rl ON rl.user.id = u.id
                                        AND rl.createdAt >= :startDate
                LEFT JOIN Comment c ON c.user.id = u.id
                                    AND c.createdAt >= :startDate
                WHERE u.deletedAt IS NULL
                GROUP BY u.id
                HAVING COALESCE(SUM(pr.score), 0) > 0
                    OR COUNT(DISTINCT rl.id) > 0
                    OR COUNT(DISTINCT c.id) > 0
                ORDER BY u.id
                """)
          .parameterValues(Map.of("period", period, "startDate", startDate));
    }
    return builder.build();
  }
}