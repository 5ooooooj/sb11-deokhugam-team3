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
          r.likeCount,
          r.commentCount
        )
        FROM Review r
        ORDER BY r.id
        """);
    } else {
      builder.queryString("""
        SELECT new com.team3.deokhugam.batch.dto.PopularReviewRawData(
          r.id,
          r.likeCount,
          r.commentCount
        )
        FROM Review r
        WHERE r.createdAt >= :startDate
        ORDER BY r.id
        """)
          .parameterValues(Map.of("startDate", startDate));
    }
    return builder.build();
  }
}
