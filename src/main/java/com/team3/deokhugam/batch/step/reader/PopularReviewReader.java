package com.team3.deokhugam.batch.step.reader;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
import com.team3.deokhugam.batch.global.DateCalculateUtil;
import com.team3.deokhugam.batch.global.Period;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularReviewReader {

  private static final int MAX_ITEM_COUNT = 100;
  private final EntityManagerFactory entityManagerFactory;

  public AbstractItemStreamItemReader<PopularReviewRawData> create(Period period) {
    return new AbstractItemStreamItemReader<>() {
      private List<PopularReviewRawData> buffer;
      private int index;

      @Override
      public void open(ExecutionContext executionContext) {
        super.open(executionContext);
        buffer = fetchData(period);
        index = 0;
      }

      @Override
      public PopularReviewRawData read() {
        if (buffer == null || index >= buffer.size()) return null;
        return buffer.get(index++);
      }
    };
  }

  private List<PopularReviewRawData> fetchData(Period period) {
    Instant startDate = DateCalculateUtil.getStartDate(period);
    Instant endDate = DateCalculateUtil.getEndDate(period);

    EntityManager em = entityManagerFactory.createEntityManager();
    try {
      if (startDate == null) {
        return em.createQuery("""
            SELECT new com.team3.deokhugam.batch.dto.PopularReviewRawData(
              r.id,
              CAST(COUNT(DISTINCT rl.id) AS int),
              CAST(COUNT(DISTINCT c.id) AS int),
              CAST((COUNT(DISTINCT rl.id) * 0.3 + COUNT(DISTINCT c.id) * 0.7) AS bigdecimal)
            )
            FROM Review r
            LEFT JOIN ReviewLike rl ON rl.review.id = r.id
            LEFT JOIN Comment c ON c.review.id = r.id
            GROUP BY r.id, r.createdAt
            HAVING COUNT(DISTINCT rl.id) > 0 OR COUNT(DISTINCT c.id) > 0
            ORDER BY (COUNT(DISTINCT rl.id) * 0.3 + COUNT(DISTINCT c.id) * 0.7) DESC,
                     r.createdAt DESC, r.id DESC
            """, PopularReviewRawData.class)
            .setMaxResults(MAX_ITEM_COUNT)
            .getResultList();
      } else {
        return em.createQuery("""
            SELECT new com.team3.deokhugam.batch.dto.PopularReviewRawData(
              r.id,
              CAST(COUNT(DISTINCT rl.id) AS int),
              CAST(COUNT(DISTINCT c.id) AS int),
              CAST((COUNT(DISTINCT rl.id) * 0.3 + COUNT(DISTINCT c.id) * 0.7) AS bigdecimal)
            )
            FROM Review r
            LEFT JOIN ReviewLike rl ON rl.review.id = r.id
              AND rl.createdAt >= :startDate AND rl.createdAt < :endDate
            LEFT JOIN Comment c ON c.review.id = r.id
              AND c.createdAt >= :startDate AND c.createdAt < :endDate
            GROUP BY r.id, r.createdAt
            HAVING COUNT(DISTINCT rl.id) > 0 OR COUNT(DISTINCT c.id) > 0
            ORDER BY (COUNT(DISTINCT rl.id) * 0.3 + COUNT(DISTINCT c.id) * 0.7) DESC,
                     r.createdAt DESC, r.id DESC
            """, PopularReviewRawData.class)
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