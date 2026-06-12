package com.team3.deokhugam.batch.step.reader;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.DateCalculateUtil;
import com.team3.deokhugam.batch.global.Period;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.stereotype.Component;

@Slf4j
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

    log.debug("🚀 [배치 범위 확인] 시작: " + startDate + " ~ 끝: " + endDate);

    EntityManager em = entityManagerFactory.createEntityManager();
    try {
      if (startDate == null) {
        return em.createQuery("""
            SELECT new com.team3.deokhugam.batch.dto.PowerUserRawData(
              u.id,
              CAST(COALESCE(SUM(pr.score), 0) AS bigdecimal),
              CAST(COUNT(DISTINCT rl.id) AS int),
              CAST(COUNT(DISTINCT c.id) AS int),
              CAST((COALESCE(SUM(pr.score), 0) * 0.5
                + COUNT(DISTINCT rl.id) * 0.2
                + COUNT(DISTINCT c.id) * 0.3) AS bigdecimal)
            )
            FROM User u
            LEFT JOIN Review r ON r.user.id = u.id
            LEFT JOIN PopularReview pr ON pr.reviewId = r.id
                                       AND pr.period = :period
            LEFT JOIN ReviewLike rl ON rl.user.id = u.id
            LEFT JOIN Comment c ON c.user.id = u.id
            WHERE u.deletedAt IS NULL
            GROUP BY u.id, u.createdAt
            HAVING COALESCE(SUM(pr.score), 0) > 0
                OR COUNT(DISTINCT rl.id) > 0
                OR COUNT(DISTINCT c.id) > 0
            ORDER BY (COALESCE(SUM(pr.score), 0) * 0.5
                + COUNT(DISTINCT rl.id) * 0.2
                + COUNT(DISTINCT c.id) * 0.3) DESC,
                u.createdAt DESC, u.id DESC
            """, PowerUserRawData.class)
            .setParameter("period", period)
            .setMaxResults(MAX_ITEM_COUNT)
            .getResultList();
      } else {
        return em.createQuery("""
            SELECT new com.team3.deokhugam.batch.dto.PowerUserRawData(
              u.id,
              CAST(COALESCE(SUM(pr.score), 0) AS bigdecimal),
              CAST(COUNT(DISTINCT rl.id) AS int),
              CAST(COUNT(DISTINCT c.id) AS int),
              CAST((COALESCE(SUM(pr.score), 0) * 0.5
                + COUNT(DISTINCT rl.id) * 0.2
                + COUNT(DISTINCT c.id) * 0.3) AS bigdecimal)
            )
            FROM User u
            LEFT JOIN Review r ON r.user.id = u.id
            LEFT JOIN PopularReview pr ON pr.reviewId = r.id
                                       AND pr.period = :period
            LEFT JOIN ReviewLike rl ON rl.user.id = u.id
                                    AND rl.createdAt >= :startDate AND rl.createdAt < :endDate
            LEFT JOIN Comment c ON c.user.id = u.id
                                AND c.createdAt >= :startDate AND c.createdAt < :endDate
            WHERE u.deletedAt IS NULL
            GROUP BY u.id, u.createdAt
            HAVING COALESCE(SUM(pr.score), 0) > 0
                OR COUNT(DISTINCT rl.id) > 0
                OR COUNT(DISTINCT c.id) > 0
            ORDER BY (COALESCE(SUM(pr.score), 0) * 0.5
                + COUNT(DISTINCT rl.id) * 0.2
                + COUNT(DISTINCT c.id) * 0.3) DESC,
                u.createdAt DESC, u.id DESC
            """, PowerUserRawData.class)
            .setParameter("period", period)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setMaxResults(MAX_ITEM_COUNT)
            .getResultList();
      }
    } finally {
      em.close();
    }
  }
}