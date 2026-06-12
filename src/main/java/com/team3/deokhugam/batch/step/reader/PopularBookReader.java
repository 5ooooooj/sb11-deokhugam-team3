package com.team3.deokhugam.batch.step.reader;

import com.team3.deokhugam.batch.global.DateCalculateUtil;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.dto.PopularBookRawData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.List;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PopularBookReader {

  private static final int MAX_ITEM_COUNT = 100;
  private final EntityManagerFactory entityManagerFactory;

  public AbstractItemStreamItemReader<PopularBookRawData> create(Period period) {
    return new AbstractItemStreamItemReader<>() {
      private List<PopularBookRawData> buffer;
      private int index;

      @Override
      public void open(@NonNull ExecutionContext executionContext) {
        super.open(executionContext);
        buffer = fetchData(period);
        index = 0;
      }

      @Override
      public PopularBookRawData read() {
        if (buffer == null || index >= buffer.size()) return null;
        return buffer.get(index++);
      }
    };
  }

  private List<PopularBookRawData> fetchData(Period period) {
    Instant startDate = DateCalculateUtil.getStartDate(period);
    Instant endDate = DateCalculateUtil.getEndDate(period);

    log.debug("🚀 [배치 범위 확인] 시작: " + startDate + " ~ 끝: " + endDate);

    try (EntityManager em = entityManagerFactory.createEntityManager()) {
      if (startDate == null) {
        return em.createQuery("""
        SELECT new com.team3.deokhugam.batch.dto.PopularBookRawData(
          r.book.id,
          CAST(COUNT(r) AS int),
          CAST(AVG(r.rating) AS bigdecimal),
          CAST((COUNT(r) * 0.4 + AVG(r.rating) * 0.6) AS bigdecimal)
        )
        FROM Review r
        GROUP BY r.book.id, r.book.createdAt
        ORDER BY (COUNT(r) * 0.4 + AVG(r.rating) * 0.6) DESC, r.book.createdAt DESC, r.book.id DESC
        """, PopularBookRawData.class)
            .setMaxResults(MAX_ITEM_COUNT)
            .getResultList();
      } else {
        return em.createQuery("""
        SELECT new com.team3.deokhugam.batch.dto.PopularBookRawData(
          r.book.id,
          CAST(COUNT(r) AS int),
          CAST(AVG(r.rating) AS bigdecimal),
          CAST((COUNT(r) * 0.4 + AVG(r.rating) * 0.6) AS bigdecimal)
        )
        FROM Review r
        WHERE r.createdAt >= :startDate AND r.createdAt < :endDate
        GROUP BY r.book.id, r.book.createdAt
        ORDER BY (COUNT(r) * 0.4 + AVG(r.rating) * 0.6) DESC, r.book.createdAt DESC, r.book.id DESC
        """, PopularBookRawData.class)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setMaxResults(MAX_ITEM_COUNT)
            .getResultList();
      }
    }
}
}