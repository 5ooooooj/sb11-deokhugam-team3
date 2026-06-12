package com.team3.deokhugam.batch.step.reader;

import com.team3.deokhugam.batch.global.DateCalculateUtil;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.dto.PopularBookRawData;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.stereotype.Component;

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

    try (EntityManager em = entityManagerFactory.createEntityManager()) {
      String sql;
      jakarta.persistence.Query query;

      if (startDate == null) {
        sql = """
                SELECT b.id, 
                       CAST(COUNT(r.id) AS integer) AS review_count,
                       CAST(AVG(r.rating) AS numeric(10,4)) AS avg_rating,
                       CAST((COUNT(r.id) * 0.4 + AVG(r.rating) * 0.6) AS numeric(10,4)) AS score
                FROM reviews r
                JOIN books b ON r.book_id = b.id
                WHERE b.deleted_at IS NULL
                GROUP BY b.id, b.created_at
                ORDER BY score DESC, b.created_at DESC, b.id DESC
                LIMIT :limit
                """;
        query = em.createNativeQuery(sql)
            .setParameter("limit", MAX_ITEM_COUNT);
      } else {
        sql = """
                SELECT b.id,
                       CAST(COUNT(r.id) AS integer) AS review_count,
                       CAST(AVG(r.rating) AS numeric(10,4)) AS avg_rating,
                       CAST((COUNT(r.id) * 0.4 + AVG(r.rating) * 0.6) AS numeric(10,4)) AS score
                FROM reviews r
                JOIN books b ON r.book_id = b.id
                WHERE r.created_at >= :startDate AND r.created_at < :endDate
                  AND b.deleted_at IS NULL
                GROUP BY b.id, b.created_at
                ORDER BY score DESC, b.created_at DESC, b.id DESC
                LIMIT :limit
                """;
        query = em.createNativeQuery(sql)
            .setParameter("startDate", startDate)
            .setParameter("endDate", endDate)
            .setParameter("limit", MAX_ITEM_COUNT);
      }

      List<Object[]> rows = query.getResultList();
      return rows.stream()
          .map(row -> new PopularBookRawData(
              row[0] instanceof UUID ? (UUID) row[0] : UUID.fromString(row[0].toString()),
              ((Number) row[1]).intValue(),
              new BigDecimal(row[2].toString()),
              new BigDecimal(row[3].toString())
          ))
          .collect(Collectors.toList());
    }
  }
}