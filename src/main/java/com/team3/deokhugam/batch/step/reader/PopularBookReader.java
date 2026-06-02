package com.team3.deokhugam.batch.step.reader;

import com.team3.deokhugam.batch.global.DateCalculateUtil;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.batch.dto.PopularBookRawData;
import jakarta.persistence.EntityManagerFactory;
import java.time.Instant;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularBookReader {

  private final EntityManagerFactory entityManagerFactory;

  public JpaPagingItemReader<PopularBookRawData> create(Period period) {
    Instant startDate = DateCalculateUtil.getStartDate(period);

    JpaPagingItemReaderBuilder<PopularBookRawData> builder =
        new JpaPagingItemReaderBuilder<PopularBookRawData>()
            .name("popularBookReader_" + period.name())
            .entityManagerFactory(entityManagerFactory)
            .pageSize(500);

    if (startDate == null) {
      // ALL_TIME 날짜조건 없음
      builder.queryString("""
        SELECT new com.team3.deokhugam.batch.dto.PopularBookRawData(
          r.bookId,
          CAST(COUNT(r) AS int),
          CAST(AVG(r.rating) AS bigdecimal )
        )
        FROM Review r
        GROUP BY r.bookId
        ORDER BY r.bookId
        """);
    } else {
      builder.queryString("""
        SELECT new com.team3.deokhugam.batch.dto.PopularBookRawData(
          r.bookId,
          CAST(COUNT(r) AS int),
          CAST(AVG(r.rating) AS bigdecimal)
        )
        FROM Review r
        WHERE r.createdAt >= :startDate
        GROUP BY r.bookId
        ORDER BY r.bookId
        """)
          .parameterValues(Map.of("startDate", startDate));
    }

    return builder.build();
  }
}
