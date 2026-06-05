package com.team3.deokhugam.batch.step.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
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
class PopularBookWriterTest {

  private PopularBookWriter popularBookWriter;

  @BeforeEach
  void setUp() {
    popularBookWriter = new PopularBookWriter();
    popularBookWriter.beforeStep(mock(StepExecution.class));
  }

  @Test
  @DisplayName("성공: chunk 아이템을 누적")
  void write_success() throws Exception {
    // given
    List<PopularBook> items = List.of(
        createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(3.0)),
        createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(5.0)),
        createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(1.0))
    );
    Chunk<PopularBook> chunk = new Chunk<>(items);
    ItemWriter<PopularBook> writer = popularBookWriter.create();

    writer.write(chunk);

    assertThat(popularBookWriter.getAccumulated()).containsExactlyElementsOf(items);
  }

  @Test
  @DisplayName("성공: 빈 chunk를 처리")
  void write_emptyChunk() throws Exception {
    // given
    Chunk<PopularBook> chunk = new Chunk<>(List.of());

    // when
    popularBookWriter.create().write(chunk);

    // then
    assertThat(popularBookWriter.getAccumulated()).isEmpty();
  }

  @Test
  @DisplayName("성공: 여러 chunk가 누적됨")
  void write_multipleChunks() throws Exception {
    PopularBook item1 = createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(3.0));
    PopularBook item2 = createPopularBook(UUID.randomUUID(), BigDecimal.valueOf(5.0));
    ItemWriter<PopularBook> writer = popularBookWriter.create();

    writer.write(new Chunk<>(List.of(item1)));
    writer.write(new Chunk<>(List.of(item2)));

    assertThat(popularBookWriter.getAccumulated()).containsExactly(item1, item2);
  }

  private PopularBook createPopularBook(UUID bookId, BigDecimal score) {
    return PopularBook.builder()
        .bookId(bookId)
        .period(Period.DAILY)
        .score(score)
        .reviewCount(0)
        .rating(BigDecimal.ZERO)
        .calculatedAt(Instant.now())
        .build();
  }
}
