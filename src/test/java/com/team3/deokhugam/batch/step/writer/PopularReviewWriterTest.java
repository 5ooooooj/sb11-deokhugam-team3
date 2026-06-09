package com.team3.deokhugam.batch.step.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.domain.dashboard.PopularReview;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@ExtendWith(MockitoExtension.class)
class PopularReviewWriterTest {

  private PopularReviewWriter popularReviewWriter;

  @BeforeEach
  void setUp() {
    popularReviewWriter = new PopularReviewWriter();
    popularReviewWriter.beforeStep(mock(StepExecution.class));
  }

  @Test
  @DisplayName("성공: chunk 아이템을 누적")
  void write_success() throws Exception {
    List<PopularReview> items = List.of(
        createPopularReview(UUID.randomUUID(), BigDecimal.valueOf(3.0)),
        createPopularReview(UUID.randomUUID(), BigDecimal.valueOf(5.0)),
        createPopularReview(UUID.randomUUID(), BigDecimal.valueOf(1.0))
    );
    Chunk<PopularReview> chunk = new Chunk<>(items);
    ItemWriter<PopularReview> writer = popularReviewWriter.create();

    writer.write(chunk);

    assertThat(popularReviewWriter.getAccumulated()).containsExactlyElementsOf(items);
  }

  @Test
  @DisplayName("성공: 빈 chunk를 처리")
  void write_emptyChunk() throws Exception {
    Chunk<PopularReview> chunk = new Chunk<>(List.of());

    popularReviewWriter.create().write(chunk);

    assertThat(popularReviewWriter.getAccumulated()).isEmpty();
  }

  @Test
  @DisplayName("성공: 여러 chunk가 누적됨")
  void write_multipleChunks() throws Exception {
    PopularReview item1 = createPopularReview(UUID.randomUUID(), BigDecimal.valueOf(3.0));
    PopularReview item2 = createPopularReview(UUID.randomUUID(), BigDecimal.valueOf(5.0));
    ItemWriter<PopularReview> writer = popularReviewWriter.create();

    writer.write(new Chunk<>(List.of(item1)));
    writer.write(new Chunk<>(List.of(item2)));

    assertThat(popularReviewWriter.getAccumulated()).containsExactly(item1, item2);
  }

  private PopularReview createPopularReview(UUID reviewId, BigDecimal score) {
    return PopularReview.builder()
        .reviewId(reviewId)
        .period(Period.DAILY)
        .score(score)
        .likeCount(0)
        .commentCount(0)
        .calculatedAt(Instant.now())
        .build();
  }
}