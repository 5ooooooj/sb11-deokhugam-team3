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

    return new JpaPagingItemReaderBuilder<PopularBookRawData>()
        .name("popularBookReader")
        .entityManagerFactory(entityManagerFactory)
        .queryString("""
            SELECT new com.deokhugam.batch.dto.PopularBookRawData(
              r.book.id,
              COUNT(r),
              AVG(r.rating)
            )
            FROM Review r
            WHERE (:startDate IS NULL OR r.createdAt >= :startDate)
            GROUP BY r.book.id
            """)
        .parameterValues(Map.of("startDate", startDate))
        .pageSize(500)
        .build();
  }

}
