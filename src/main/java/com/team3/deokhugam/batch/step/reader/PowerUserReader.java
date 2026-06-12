package com.team3.deokhugam.batch.step.reader;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.DateCalculateUtil;
import com.team3.deokhugam.batch.global.Period;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PowerUserReader {

  private static final int MAX_ITEM_COUNT = 100;
  private final EntityManagerFactory entityManagerFactory;

  public AbstractItemStreamItemReader<PowerUserRawData> create(Period period) {
    return new AbstractItemStreamItemReader<>() {
      private List<PowerUserRawData> buffer;
      private int index;

      @Override
      public void open(ExecutionContext executionContext) {
        super.open(executionContext);
        buffer = fetchData(period);
        index = 0;
      }

      @Override
      public PowerUserRawData read() {
        if (buffer == null || index >= buffer.size()) return null;
        return buffer.get(index++);
      }
    };
  }

  private List<PowerUserRawData> fetchData(Period period) {
    Instant startDate = DateCalculateUtil.getStartDate(period);
    Instant endDate = DateCalculateUtil.getEndDate(period);

    EntityManager em = entityManagerFactory.createEntityManager();
    try {
      if (startDate == null) {
        String sql = """
                WITH review_scores AS (
                    SELECT r.user_id, COALESCE(SUM(pr.score), 0) AS score_sum
                    FROM reviews r
                    JOIN popular_reviews pr ON pr.review_id = r.id AND pr.period = :period
                    GROUP BY r.user_id
                ),
                like_counts AS (
                    SELECT rl.user_id, COUNT(*) AS cnt
                    FROM review_likes rl
                    GROUP BY rl.user_id
                ),
                comment_counts AS (
                    SELECT c.user_id, COUNT(*) AS cnt
                    FROM comments c
                    GROUP BY c.user_id
                )
                SELECT u.id,
                       CAST(COALESCE(rs.score_sum, 0) AS numeric),
                       CAST(COALESCE(lc.cnt, 0) AS integer),
                       CAST(COALESCE(cc.cnt, 0) AS integer),
                       CAST(COALESCE(rs.score_sum,0)*0.5
                           + COALESCE(lc.cnt,0)*0.2
                           + COALESCE(cc.cnt,0)*0.3 AS numeric)
                FROM users u
                LEFT JOIN review_scores rs ON rs.user_id = u.id
                LEFT JOIN like_counts lc ON lc.user_id = u.id
                LEFT JOIN comment_counts cc ON cc.user_id = u.id
                WHERE u.deleted_at IS NULL
                  AND (COALESCE(rs.score_sum,0)*0.5
                      + COALESCE(lc.cnt,0)*0.2
                      + COALESCE(cc.cnt,0)*0.3) > 0
                ORDER BY (COALESCE(rs.score_sum,0)*0.5
                         + COALESCE(lc.cnt,0)*0.2
                         + COALESCE(cc.cnt,0)*0.3) DESC,
                         u.created_at, u.id
                LIMIT :limit
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
            .setParameter("period", period.name())
            .setParameter("limit", MAX_ITEM_COUNT)
            .getResultList();

        return mapRows(rows);

      } else {
        String sql = """
                WITH review_scores AS (
                    SELECT r.user_id, COALESCE(SUM(pr.score), 0) AS score_sum
                    FROM reviews r
                    JOIN popular_reviews pr ON pr.review_id = r.id AND pr.period = :period
                    GROUP BY r.user_id
                ),
                like_counts AS (
                    SELECT rl.user_id, COUNT(*) AS cnt
                    FROM review_likes rl
                    WHERE rl.created_at >= :startDate AND rl.created_at < :endDate
                    GROUP BY rl.user_id
                ),
                comment_counts AS (
                    SELECT c.user_id, COUNT(*) AS cnt
                    FROM comments c
                    WHERE c.created_at >= :startDate AND c.created_at < :endDate
                    GROUP BY c.user_id
                )
                SELECT u.id,
                       CAST(COALESCE(rs.score_sum, 0) AS numeric),
                       CAST(COALESCE(lc.cnt, 0) AS integer),
                       CAST(COALESCE(cc.cnt, 0) AS integer),
                       CAST(COALESCE(rs.score_sum,0)*0.5
                           + COALESCE(lc.cnt,0)*0.2
                           + COALESCE(cc.cnt,0)*0.3 AS numeric)
                FROM users u
                LEFT JOIN review_scores rs ON rs.user_id = u.id
                LEFT JOIN like_counts lc ON lc.user_id = u.id
                LEFT JOIN comment_counts cc ON cc.user_id = u.id
                WHERE u.deleted_at IS NULL
                  AND (COALESCE(rs.score_sum,0)*0.5
                      + COALESCE(lc.cnt,0)*0.2
                      + COALESCE(cc.cnt,0)*0.3) > 0
                ORDER BY (COALESCE(rs.score_sum,0)*0.5
                         + COALESCE(lc.cnt,0)*0.2
                         + COALESCE(cc.cnt,0)*0.3) DESC,
                         u.created_at, u.id
                LIMIT :limit
                """;

        List<Object[]> rows = em.createNativeQuery(sql)
            .setParameter("period", period.name())
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setParameter("limit", MAX_ITEM_COUNT)
            .getResultList();

        return mapRows(rows);
      }
    } finally {
      em.close();
    }
  }

  private List<PowerUserRawData> mapRows(List<Object[]> rows) {
    return rows.stream()
        .map(row -> new PowerUserRawData(
            row[0] instanceof UUID ? (UUID) row[0] : UUID.fromString(row[0].toString()),
            new BigDecimal(row[1].toString()),
            ((Number) row[2]).intValue(),
            ((Number) row[3]).intValue(),
            new BigDecimal(row[4].toString())
        ))
        .collect(Collectors.toList());
  }
}