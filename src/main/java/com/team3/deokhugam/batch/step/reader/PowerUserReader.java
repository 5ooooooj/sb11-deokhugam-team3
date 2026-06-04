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
          u.userId,
          COALESCE(SUM(pr.score), 0),
          COALESCE(SUM(rl.likeCount), 0),
          COALESCE(SUM(c.commentCount), 0) 
        )
        FROM (SELECT DISTINCT r.userId FROM Review r) u
        LEFT JOIN (
          SELECT r.userId AS userId, SUM(pr.score) AS score
          FROM Review r
          JOIN PopularReview pr ON pr.reviewID = r.id
                                AND pr.period = :period
          GROUP BY r.userId
        ) pr ON pr.userId = u.userId
        LEFT JOIN (
          SELECT rl.userId AS userId, COUNT(rl.id) AS likeCount
          FROM ReviewLike rl
          GROUP BY rl.userId
        ) rl ON rl.userId = u.userId
        LEFT JOIN (
          SELECT c.user.id AS userId, COUNT(c.id) AS commentCount
          FROM Comment c
          GROUP BY c.user.id
        ) c ON c.userId = u.userId
        GROUP BY u.userId
        ORDER BY u.userId
        """)
          .parameterValues(Map.of("period", period));
    } else {
        builder.queryString("""
          SELECT new com.team3.deokhugam.batch.dto.PowerUserRawData(
            u.userId,
            COALESCE(SUM(pr.score), 0),
            COALESCE(SUM(rl.likeCount), 0),
            COALESCE(SUM(c.commentCount), 0)
          )
          FROM (SELECT DISTINCT r.userId FROM Review r
                WHERE r.createdAt >= :startDate) u
          LEFT JOIN (
          SELECT r.userId AS userId, SUM(pr.score) AS score
          FROM Review r
          JOIN PopularReview pr ON pr.reviewID = r.id
                                AND pr.period = :period
          GROUP BY r.userId
        ) pr ON pr.userId = u.userId
        LEFT JOIN (
          SELECT rl.userId AS userId, COUNT(rl.id) AS likeCount
          FROM ReviewLike rl
          WHERE rl.createAt >= :startDate
          GROUP BY rl.userId
        ) rl ON rl.userId = u.userId
        LEFT JOIN (
          SELECT c.user.id AS userId, COUNT(c.id) AS commentCount
          FROM Comment c
          GROUP BY c.createdAt >= :startDate
        ) c ON c.userId = u.userId
        GROUP BY u.userId
        ORDER BY u.userId
        """)
            .parameterValues(Map.of("period", period, "startDate", startDate));
    }
    return builder.build();
  }

}
