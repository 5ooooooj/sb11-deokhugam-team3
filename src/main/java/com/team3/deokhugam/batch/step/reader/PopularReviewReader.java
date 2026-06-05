package com.team3.deokhugam.batch.step.reader;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
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
public class PopularReviewReader {

  private final EntityManagerFactory entityManagerFactory;

  public JpaPagingItemReader<PopularReviewRawData> create(Period period) {
    Instant startDate = DateCalculateUtil.getStartDate(period);

    JpaPagingItemReaderBuilder<PopularReviewRawData> builder =
        new JpaPagingItemReaderBuilder<PopularReviewRawData>()
            .name("popularReviewReader_" + period.name())
            .entityManagerFactory(entityManagerFactory)
            .pageSize(500);

    if (startDate == null) {
      // ALL_TIME 날짜조건 없음
      builder.queryString("""
        SELECT new com.team3.deokhugam.batch.dto.PopularReviewRawData(
          r.id,
          CAST(COUNT(DISTINCT rl.id) AS int),
          CAST(COUNT(DISTINCT c.id) AS int)
        )
        FROM Review r
        LEFT JOIN ReviewLike  rl ON rl.review.id = r.id
        LEFT JOIN Comment  c ON c.review.id = r.id
        GROUP BY r.id
        HAVING COUNT(DISTINCT rl.id) > 0 OR COUNT(DISTINCT c.id) > 0
        ORDER BY r.id
        """);
    } else {
      builder.queryString("""
        SELECT new com.team3.deokhugam.batch.dto.PopularReviewRawData(
          r.id,
          CAST(COUNT(DISTINCT rl.id) AS int),
          CAST(COUNT(DISTINCT c.id) AS int)
        )
        FROM Review r
        LEFT JOIN ReviewLike  rl ON rl.review.id = r.id
          AND rl.createdAt >= :startDate
        LEFT JOIN Comment  c ON c.review.id = r.id
          AND c.createdAt >= :startDate
        GROUP BY r.id
        HAVING COUNT(DISTINCT rl.id) > 0 OR COUNT(DISTINCT c.id) > 0
        ORDER BY r.id
        """)
          .parameterValues(Map.of("startDate", startDate));
    }
    return builder.build();
  }
}
