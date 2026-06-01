package com.team3.deokhugam.batch.step.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.batch.item.Chunk;

@ExtendWith(MockitoExtension.class)
class PopularBookWriterTest {

  @Mock
  private PopularBookRepository popularBookRepository;

  @Captor
  private ArgumentCaptor<List<PopularBook>> captor;

  private PopularBookWriter popularBookWriter;

  @BeforeEach
  void setUp() {
    popularBookWriter = new PopularBookWriter(popularBookRepository);
  }

  @Test
  @DisplayName("성공: 점수 내림차순으로 순위를 부여하고 저장")
  void write_success() throws Exception {
    // given
    List<PopularBook> items = List.of(
        createPopularBook(UUID.randomUUID(), 3.0),
        createPopularBook(UUID.randomUUID(), 5.0),
        createPopularBook(UUID.randomUUID(), 1.0)
    );
    Chunk<PopularBook> chunk = new Chunk<>(items);

    // when
    popularBookWriter.create(Period.DAILY).write(chunk);

    // then
    verify(popularBookRepository).deleteByPeriod(Period.DAILY);
    verify(popularBookRepository).saveAll(captor.capture());

    List<PopularBook> saved = captor.getValue();
    assertThat(saved.get(0).getScore()).isEqualTo(5.0); // 1위
    assertThat(saved.get(0).getRank()).isEqualTo(1);
    assertThat(saved.get(1).getScore()).isEqualTo(3.0); // 2위
    assertThat(saved.get(1).getRank()).isEqualTo(2);
    assertThat(saved.get(2).getScore()).isEqualTo(1.0); // 3위
    assertThat(saved.get(2).getRank()).isEqualTo(3);
  }

  @Test
  @DisplayName("성공: 저장 전 해당 period 기존 데이터를 삭제")
  void write_deleteBeforeSave() throws Exception {
    // given
    Chunk<PopularBook> chunk = new Chunk<>(List.of(
        createPopularBook(UUID.randomUUID(), 3.0)
    ));

    // when
    popularBookWriter.create(Period.DAILY).write(chunk);

    // then - deleteByPeriod가 saveAll보다 먼저 호출됐는지 순서 검증
    InOrder inOrder = inOrder(popularBookRepository);
    inOrder.verify(popularBookRepository).deleteByPeriod(Period.DAILY);
    inOrder.verify(popularBookRepository).saveAll(any());
  }

  @Test
  @DisplayName("성공: 동점일 때 순위를 동일하게 부여")
  void write_sameScore() throws Exception {
    // given
    List<PopularBook> items = List.of(
        createPopularBook(UUID.randomUUID(), 3.0),
        createPopularBook(UUID.randomUUID(), 3.0),
        createPopularBook(UUID.randomUUID(), 1.0)
    );
    Chunk<PopularBook> chunk = new Chunk<>(items);

    // when
    popularBookWriter.create(Period.DAILY).write(chunk);

    // then
    verify(popularBookRepository).saveAll(captor.capture());

    List<PopularBook> saved = captor.getValue();
    assertThat(saved.get(0).getRank()).isEqualTo(1);
    assertThat(saved.get(1).getRank()).isEqualTo(1); // 동점 → 같은 순위
    assertThat(saved.get(2).getRank()).isEqualTo(3); // 다음 순위는 3
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
