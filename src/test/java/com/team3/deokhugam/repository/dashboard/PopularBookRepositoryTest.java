package com.team3.deokhugam.repository.dashboard;

import static com.team3.deokhugam.domain.book.BookTestFactory.book;
import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.dto.dashboard.PopularBookDto;
import com.team3.deokhugam.repository.BaseRepositoryTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

@DisplayName("PopularBookRepository 테스트")
class PopularBookRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private PopularBookRepository popularBookRepository;

  @Autowired
  private TestEntityManager em;

  private UUID bookId1;
  private UUID bookId2;

  @BeforeEach
  void setUp() {
    Book book1 = em.persistAndFlush(book()
        .title("도서1")
        .author("저자1")
        .description("설명1")
        .publisher("출판사1")
        .publishedDate(LocalDate.of(2013, 12, 30))
        .isbn("9788960177758")
        .thumbnailUrl("https://example.com/book1.jpg")
        .build());

    Book book2 = em.persistAndFlush(book()
        .title("도서2")
        .author("저자2")
        .description("설명2")
        .publisher("출판사2")
        .publishedDate(LocalDate.of(2013, 12, 31))
        .isbn("9788960177750")
        .thumbnailUrl("https://example.com/book2.jpg")
        .build());

    bookId1 = book1.getId();
    bookId2 = book2.getId();

    em.persist(PopularBook.builder()
        .bookId(bookId1)
        .period(Period.DAILY)
        .score(BigDecimal.valueOf(90))
        .ranking(1)
        .reviewCount(10)
        .rating(BigDecimal.valueOf(4.5))
        .calculatedAt(Instant.now())
        .build());

    em.persist(PopularBook.builder()
        .bookId(bookId2)
        .period(Period.DAILY)
        .score(BigDecimal.valueOf(80))
        .ranking(2)
        .reviewCount(8)
        .rating(BigDecimal.valueOf(4.0))
        .calculatedAt(Instant.now())
        .build());

    em.persist(PopularBook.builder()
        .bookId(bookId1)
        .period(Period.WEEKLY)
        .score(BigDecimal.valueOf(95))
        .ranking(1)
        .reviewCount(15)
        .rating(BigDecimal.valueOf(4.8))
        .calculatedAt(Instant.now())
        .build());

    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("성공: DAILY period 인기 도서 rank 오름차순 조회")
  void findPopularBooksByPeriod_daily_success() {
    List<PopularBookDto> result = popularBookRepository
        .findPopularBooksByPeriod(Period.DAILY, PageRequest.of(0, 10));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).rank()).isEqualTo(1);
    assertThat(result.get(1).rank()).isEqualTo(2);
  }

  @Test
  @DisplayName("성공: period 필터 - DAILY 조회 시 WEEKLY 데이터 미포함")
  void findPopularBooksByPeriod_filtersByPeriod() {
    List<PopularBookDto> result = popularBookRepository
        .findPopularBooksByPeriod(Period.DAILY, PageRequest.of(0, 10));

    assertThat(result).allMatch(dto -> dto.period() == Period.DAILY);
  }

  @Test
  @DisplayName("성공: limit 적용 - 1개만 조회")
  void findPopularBooksByPeriod_limitApplied() {
    List<PopularBookDto> result = popularBookRepository
        .findPopularBooksByPeriod(Period.DAILY, PageRequest.of(0, 1));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).rank()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 데이터 없는 period 조회 시 빈 리스트 반환")
  void findPopularBooksByPeriod_emptyResult() {
    List<PopularBookDto> result = popularBookRepository
        .findPopularBooksByPeriod(Period.MONTHLY, PageRequest.of(0, 10));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("성공: Book 정보가 함께 조회된다")
  void findPopularBooksByPeriod_includesBookInfo() {
    List<PopularBookDto> result = popularBookRepository
        .findPopularBooksByPeriod(Period.DAILY, PageRequest.of(0, 10));

    assertThat(result.get(0).title()).isEqualTo("도서1");
    assertThat(result.get(0).author()).isEqualTo("저자1");
  }

  @Test
  @DisplayName("성공: countByPeriod - DAILY 2개 반환")
  void countByPeriod_success() {
    int count = popularBookRepository.countByPeriod(Period.DAILY);

    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("성공: countByPeriod - 데이터 없는 period는 0 반환")
  void countByPeriod_empty() {
    int count = popularBookRepository.countByPeriod(Period.MONTHLY);

    assertThat(count).isEqualTo(0);
  }

  @Test
  @DisplayName("성공: 동순위 인기 도서는 도서 등록일 최신 순(createdAt Desc)으로 정렬")
  void findPopularBooksByPeriod_sameRank_orderedByBookCreatedAtDesc() {
    // given: bookId2를 먼저, bookId1을 나중에 persist해서 순서 역전 확인
    em.persist(PopularBook.builder()
        .bookId(bookId2)
        .period(Period.MONTHLY)
        .score(BigDecimal.valueOf(100))
        .ranking(1)
        .reviewCount(10)
        .rating(BigDecimal.valueOf(4.5))
        .calculatedAt(Instant.now())
        .build());

    em.persist(PopularBook.builder()
        .bookId(bookId1)
        .period(Period.MONTHLY)
        .score(BigDecimal.valueOf(100))
        .ranking(1)
        .reviewCount(10)
        .rating(BigDecimal.valueOf(4.5))
        .calculatedAt(Instant.now())
        .build());

    em.flush();
    em.clear();

    // when
    List<PopularBookDto> result = popularBookRepository
        .findPopularBooksByPeriod(Period.MONTHLY, PageRequest.of(0, 10));

    // then: book2이 더 나중에 등록됐으므로 앞에 와야 함
    assertThat(result).hasSize(2);
    assertThat(result.get(0).bookId()).isEqualTo(bookId2);
    assertThat(result.get(1).bookId()).isEqualTo(bookId1);
  }

}
