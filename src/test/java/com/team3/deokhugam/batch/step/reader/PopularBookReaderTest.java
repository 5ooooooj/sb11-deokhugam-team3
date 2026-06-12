package com.team3.deokhugam.batch.step.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.support.AbstractItemStreamItemReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class PopularBookReaderTest {

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private PopularBookReader popularBookReader;

  private TransactionTemplate transactionTemplate;

  // 기준점 계산을 위한 한국 시간(KST) 기준의 어제 시간대 정의
  private final Instant kstYesterday = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
      .minusDays(1)
      .atZone(ZoneId.of("Asia/Seoul"))
      .toInstant();

  @BeforeEach
  void setUp() {
    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @AfterEach
  void tearDown() {
    transactionTemplate.execute(status -> {
      reviewRepository.deleteAll();
      bookRepository.deleteAll();
      userRepository.deleteAll();
      return null;
    });
  }

  private User saveUser() {
    return transactionTemplate.execute(status ->
        userRepository.save(
            new User("reader-" + UUID.randomUUID() + "@test.com", "배치테스터", "Password1!")));
  }

  private Book saveBook() {
    return transactionTemplate.execute(status ->
        bookRepository.save(new Book(
            UUID.randomUUID(), "테스트 도서", "테스트 저자", "테스트 설명",
            "테스트 출판사", LocalDate.of(2026, 1, 1), null, null)));
  }

  private void saveReviewWithCreatedAt(User user, Book book, int rating, String content, Instant createdAt) {
    transactionTemplate.execute(status -> {
      Review review = reviewRepository.save(Review.create(user, book, rating, content));
      entityManager.createQuery(
              "UPDATE Review r SET r.createdAt = :createdAt WHERE r.id = :id")
          .setParameter("createdAt", createdAt)
          .setParameter("id", review.getId())
          .executeUpdate();
      return null;
    });
  }

  private List<PopularBookRawData> readAll(Period period) throws Exception {
    AbstractItemStreamItemReader<PopularBookRawData> reader = popularBookReader.create(period);
    reader.open(new ExecutionContext());

    List<PopularBookRawData> results = new ArrayList<>();
    PopularBookRawData item;
    while ((item = reader.read()) != null) {
      results.add(item);
    }

    reader.close();
    return results;
  }

  @Test
  @DisplayName("성공: 기간 내 리뷰를 도서별로 집계")
  void read_daily_success() throws Exception {
    // given
    User user = saveUser();
    Book book = saveBook();
    // 오늘이 아닌 '어제(kstYesterday)' 범위 내로 리뷰 생성 시간 조정
    saveReviewWithCreatedAt(user, book, 4, "리뷰1", kstYesterday.plus(1, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book, 2, "리뷰2", kstYesterday.plus(2, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book, 5, "그저께 리뷰", kstYesterday.minus(1, ChronoUnit.DAYS));

    // when
    List<PopularBookRawData> results = readAll(Period.DAILY);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).bookId()).isEqualTo(book.getId());
    assertThat(results.get(0).reviewCount()).isEqualTo(2);
    assertThat(results.get(0).ratingAvg()).isCloseTo(BigDecimal.valueOf(3.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 논리 삭제된 리뷰도 집계에 포함")
  void read_includeDeletedReview() throws Exception {
    // given
    User user = saveUser();
    Book book = saveBook();

    // 기본 엔티티 매핑 저장 후, 일괄 업데이트를 통해 '어제' 날짜로 생성일 세팅
    transactionTemplate.execute(status -> {
      Review r1 = reviewRepository.save(Review.create(user, book, 4, "리뷰"));
      Review r2 = reviewRepository.save(Review.create(user, book, 2, "삭제된 리뷰"));

      entityManager.createQuery("UPDATE Review r SET r.createdAt = :createdAt WHERE r.id IN (:id1, :id2)")
          .setParameter("createdAt", kstYesterday.plus(1, ChronoUnit.HOURS))
          .setParameter("id1", r1.getId())
          .setParameter("id2", r2.getId())
          .executeUpdate();

      entityManager.createQuery("UPDATE Review r SET r.deletedAt = :deletedAt WHERE r.id = :id")
          .setParameter("deletedAt", Instant.now())
          .setParameter("id", r2.getId())
          .executeUpdate();
      return null;
    });

    // when
    List<PopularBookRawData> results = readAll(Period.DAILY);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).reviewCount()).isEqualTo(2);
    assertThat(results.get(0).ratingAvg()).isCloseTo(BigDecimal.valueOf(3.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: ALL_TIME은 전체 기간 집계")
  void read_allTime_success() throws Exception {
    // given
    User user = saveUser();
    Book book = saveBook();
    saveReviewWithCreatedAt(user, book, 5, "1년 전 리뷰", Instant.now().minus(365, ChronoUnit.DAYS));
    saveReviewWithCreatedAt(user, book, 3, "어제 리뷰1", kstYesterday.plus(1, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book, 4, "어제 리뷰2", kstYesterday.plus(2, ChronoUnit.HOURS));

    // when
    List<PopularBookRawData> results = readAll(Period.ALL_TIME);

    // then
    assertThat(results).hasSize(1);
    assertThat(results.get(0).reviewCount()).isEqualTo(3);
    assertThat(results.get(0).ratingAvg()).isCloseTo(BigDecimal.valueOf(4.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 기간 내 리뷰 없으면 빈 결과 반환")
  void read_noReviewInPeriod() throws Exception {
    // given
    User user = saveUser();
    Book book = saveBook();
    // 그저께 리뷰 등록 (어제 범위인 DAILY 조건에 미달)
    saveReviewWithCreatedAt(user, book, 5, "오래된 리뷰", kstYesterday.minus(1, ChronoUnit.DAYS));

    // when
    List<PopularBookRawData> results = readAll(Period.DAILY);

    // then
    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("성공: 여러 도서의 리뷰를 각각 집계")
  void read_multipleBooks() throws Exception {
    // given
    User user = saveUser();
    Book book1 = saveBook();
    Book book2 = saveBook();
    saveReviewWithCreatedAt(user, book1, 5, "리뷰1", kstYesterday.plus(1, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book1, 3, "리뷰2", kstYesterday.plus(2, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book2, 4, "리뷰3", kstYesterday.plus(3, ChronoUnit.HOURS));

    // when
    List<PopularBookRawData> results = readAll(Period.DAILY);

    // then
    assertThat(results).hasSize(2);
    PopularBookRawData book1Result = results.stream()
        .filter(r -> r.bookId().equals(book1.getId()))
        .findFirst().orElseThrow();
    assertThat(book1Result.reviewCount()).isEqualTo(2);
    assertThat(book1Result.ratingAvg()).isCloseTo(BigDecimal.valueOf(4.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 결과가 점수 내림차순으로 정렬")
  void read_orderedByScoreDesc() throws Exception {
    // given
    User user1 = saveUser();
    User user2 = saveUser();
    User user3 = saveUser();
    User user4 = saveUser();
    Book book1 = saveBook();
    Book book2 = saveBook();

    // book1: 리뷰 1개, 평점 5 → score = 1*0.4 + 5*0.6 = 3.4
    saveReviewWithCreatedAt(user1, book1, 5, "리뷰1", kstYesterday.plus(1, ChronoUnit.HOURS));

    // book2: 리뷰 3개, 평균 3.0 → score = 3*0.4 + 3.0*0.6 = 3.0
    saveReviewWithCreatedAt(user2, book2, 3, "리뷰2", kstYesterday.plus(1, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user3, book2, 3, "리뷰3", kstYesterday.plus(2, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user4, book2, 3, "리뷰4", kstYesterday.plus(3, ChronoUnit.HOURS));

    // when
    List<PopularBookRawData> results = readAll(Period.DAILY);

    // then
    assertThat(results).hasSize(2);
    assertThat(results.get(0).bookId()).isEqualTo(book1.getId());
    assertThat(results.get(1).bookId()).isEqualTo(book2.getId());
  }

  @Test
  @DisplayName("성공: MAX_ITEM_COUNT 초과 시 상위 100개만 반환")
  void read_maxItemCount() throws Exception {
    // given
    User user = saveUser();

    // 대량 생성 시에도 Review의 기본생성 시점이 오늘이 되므로, 강제로 '어제' 날짜로 일괄 업데이트 진행
    transactionTemplate.execute(status -> {
      List<UUID> reviewIds = new ArrayList<>();
      for (int i = 0; i < 105; i++) {
        Book book = bookRepository.save(new Book(
            UUID.randomUUID(), "도서" + i, "저자", "설명",
            "출판사", LocalDate.of(2026, 1, 1), null, null));
        Review review = reviewRepository.save(Review.create(user, book, (i % 5) + 1, "리뷰" + i));
        reviewIds.add(review.getId());
      }

      // 한 번에 '어제' 날짜 범위로 밀어넣기
      entityManager.createQuery("UPDATE Review r SET r.createdAt = :createdAt WHERE r.id IN :ids")
          .setParameter("createdAt", kstYesterday.plus(2, ChronoUnit.HOURS))
          .setParameter("ids", reviewIds)
          .executeUpdate();
      return null;
    });

    // when
    List<PopularBookRawData> results = readAll(Period.DAILY);

    // then
    assertThat(results).hasSize(100);
  }

  @Test
  @DisplayName("성공: 동점 도서는 최신 등록 도서가 우선 정렬")
  void read_sameScore_orderedByCreatedAtDesc() throws Exception {
    // given
    User user1 = saveUser();
    User user2 = saveUser();
    User user3 = saveUser();

    // 세 도서 모두 리뷰 1개, 평점 5 → score = 1*0.4 + 5*0.6 = 3.4 동점
    Book oldBook = saveBook();    // 가장 오래된 도서
    Book midBook = saveBook();    // 중간
    Book newBook = saveBook();    // 가장 최신 도서

    // createdAt을 강제로 다르게 세팅
    transactionTemplate.execute(status -> {
      entityManager.createQuery("UPDATE Book b SET b.createdAt = :createdAt WHERE b.id = :id")
          .setParameter("createdAt", Instant.now().minus(10, ChronoUnit.DAYS))
          .setParameter("id", oldBook.getId())
          .executeUpdate();
      entityManager.createQuery("UPDATE Book b SET b.createdAt = :createdAt WHERE b.id = :id")
          .setParameter("createdAt", Instant.now().minus(5, ChronoUnit.DAYS))
          .setParameter("id", midBook.getId())
          .executeUpdate();
      entityManager.createQuery("UPDATE Book b SET b.createdAt = :createdAt WHERE b.id = :id")
          .setParameter("createdAt", Instant.now().minus(1, ChronoUnit.DAYS))
          .setParameter("id", newBook.getId())
          .executeUpdate();
      return null;
    });

    saveReviewWithCreatedAt(user1, oldBook, 5, "오래된 도서 리뷰", kstYesterday.plus(1, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user2, midBook, 5, "중간 도서 리뷰", kstYesterday.plus(2, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user3, newBook, 5, "최신 도서 리뷰", kstYesterday.plus(3, ChronoUnit.HOURS));

    // when
    List<PopularBookRawData> results = readAll(Period.ALL_TIME);

    // then
    assertThat(results).hasSize(3);
    // 동점이면 최신 등록 도서 우선 → newBook > midBook > oldBook
    assertThat(results.get(0).bookId()).isEqualTo(newBook.getId());
    assertThat(results.get(1).bookId()).isEqualTo(midBook.getId());
    assertThat(results.get(2).bookId()).isEqualTo(oldBook.getId());
  }
}