package com.team3.deokhugam.batch.step.writer;

import static org.mockito.Mockito.verify;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;
import org.springframework.batch.item.ItemWriter;

@ExtendWith(MockitoExtension.class)
class PopularBookWriterTest {

  @Mock
  private PopularBookRepository popularBookRepository;

  private PopularBookWriter popularBookWriter;

  @BeforeEach
  void setUp() {
    popularBookWriter = new PopularBookWriter(popularBookRepository);
  }

  @Test
  @DisplayName("성공: chunk 아이템을 저장")
  void write_success() throws Exception {
    // given
    List<PopularBook> items = List.of(
        createPopularBook(UUID.randomUUID(), 3.0),
        createPopularBook(UUID.randomUUID(), 5.0),
        createPopularBook(UUID.randomUUID(), 1.0)
    );
    Chunk<PopularBook> chunk = new Chunk<>(items);
    ItemWriter<PopularBook> writer = popularBookWriter.create(Period.DAILY);

    writer.write(chunk);

    verify(popularBookRepository).saveAll(items);
  }

  @Test
  @DisplayName("성공: 데이터가 없을 때 삭제만 수행")
  void write_emptyChunk() throws Exception {
    // given
    Chunk<PopularBook> chunk = new Chunk<>(List.of());

    // when
    popularBookWriter.create(Period.DAILY).write(chunk);

    // then
    verify(popularBookRepository).deleteByPeriod(Period.DAILY);
    verify(popularBookRepository).saveAll(List.of());
  }

  private PopularBook createPopularBook(UUID bookId, double score) {
    return PopularBook.builder()
        .bookId(bookId)
        .period(Period.DAILY)
        .score(score)
        .reviewCount(0)
        .rating(0.0)
        .calculatedAt(Instant.now())
        .build();
  }
}
