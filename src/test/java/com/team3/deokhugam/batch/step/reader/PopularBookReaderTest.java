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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
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
    AbstractItemStreamItemReader<PopularBookRawData> reader =
         popularBookReader.create(period);
    reader.open(new ExecutionContext());  // open 호출 추가

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
    User user = saveUser();
    Book book = saveBook();
    saveReviewWithCreatedAt(user, book, 4, "리뷰1", Instant.now().minus(1, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book, 2, "리뷰2", Instant.now().minus(2, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book, 5, "오래된 리뷰", Instant.now().minus(2, ChronoUnit.DAYS));

    List<PopularBookRawData> results = readAll(Period.DAILY);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).bookId()).isEqualTo(book.getId());
    assertThat(results.get(0).reviewCount()).isEqualTo(2);
    assertThat(results.get(0).ratingAvg()).isCloseTo(BigDecimal.valueOf(3.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 논리 삭제된 리뷰도 집계에 포함")
  void read_includeDeletedReview() throws Exception {
    User user = saveUser();
    Book book = saveBook();

    transactionTemplate.execute(status -> {
      reviewRepository.save(Review.create(user, book, 4, "리뷰"));
      Review deletedReview = reviewRepository.save(Review.create(user, book, 2, "삭제된 리뷰"));
      entityManager.createQuery(
              "UPDATE Review r SET r.deletedAt = :deletedAt WHERE r.id = :id")
          .setParameter("deletedAt", Instant.now())
          .setParameter("id", deletedReview.getId())
          .executeUpdate();
      return null;
    });

    List<PopularBookRawData> results = readAll(Period.DAILY);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).reviewCount()).isEqualTo(2);
    assertThat(results.get(0).ratingAvg()).isCloseTo(BigDecimal.valueOf(3.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: ALL_TIME은 전체 기간 집계")
  void read_allTime_success() throws Exception {
    User user = saveUser();
    Book book = saveBook();
    saveReviewWithCreatedAt(user, book, 5, "1년 전 리뷰", Instant.now().minus(365, ChronoUnit.DAYS));
    saveReviewWithCreatedAt(user, book, 3, "최근 리뷰1", Instant.now().minus(1, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book, 4, "최근 리뷰2", Instant.now().minus(2, ChronoUnit.HOURS));

    List<PopularBookRawData> results = readAll(Period.ALL_TIME);

    assertThat(results).hasSize(1);
    assertThat(results.get(0).reviewCount()).isEqualTo(3);
    assertThat(results.get(0).ratingAvg()).isCloseTo(BigDecimal.valueOf(4.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 기간 내 리뷰 없으면 빈 결과 반환")
  void read_noReviewInPeriod() throws Exception {
    User user = saveUser();
    Book book = saveBook();
    saveReviewWithCreatedAt(user, book, 5, "오래된 리뷰", Instant.now().minus(2, ChronoUnit.DAYS));

    List<PopularBookRawData> results = readAll(Period.DAILY);

    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("성공: 여러 도서의 리뷰를 각각 집계")
  void read_multipleBooks() throws Exception {
    User user = saveUser();
    Book book1 = saveBook();
    Book book2 = saveBook();
    saveReviewWithCreatedAt(user, book1, 5, "리뷰1", Instant.now().minus(1, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book1, 3, "리뷰2", Instant.now().minus(2, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user, book2, 4, "리뷰3", Instant.now().minus(3, ChronoUnit.HOURS));

    List<PopularBookRawData> results = readAll(Period.DAILY);

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
    User user1 = saveUser();
    User user2 = saveUser();
    User user3 = saveUser();
    User user4 = saveUser();
    Book book1 = saveBook();
    Book book2 = saveBook();

    // book1: 리뷰 1개, 평점 5 → score = 1*0.4 + 5*0.6 = 3.4
    saveReviewWithCreatedAt(user1, book1, 5, "리뷰1", Instant.now().minus(1, ChronoUnit.HOURS));

    // book2: 리뷰 3개, 평균 3.0 → score = 3*0.4 + 3.0*0.6 = 3.0
    saveReviewWithCreatedAt(user2, book2, 3, "리뷰2", Instant.now().minus(1, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user3, book2, 3, "리뷰3", Instant.now().minus(2, ChronoUnit.HOURS));
    saveReviewWithCreatedAt(user4, book2, 3, "리뷰4", Instant.now().minus(3, ChronoUnit.HOURS));

    List<PopularBookRawData> results = readAll(Period.DAILY);

    assertThat(results).hasSize(2);
    assertThat(results.get(0).bookId()).isEqualTo(book1.getId());
    assertThat(results.get(1).bookId()).isEqualTo(book2.getId());
  }

  @Test
  @DisplayName("성공: MAX_ITEM_COUNT 초과 시 상위 100개만 반환")
  void read_maxItemCount() throws Exception {
    User user = saveUser();

    transactionTemplate.execute(status -> {
      for (int i = 0; i < 105; i++) {
        Book book = bookRepository.save(new Book(
            UUID.randomUUID(), "도서" + i, "저자", "설명",
            "출판사", LocalDate.of(2026, 1, 1), null, null));
        reviewRepository.save(Review.create(user, book, (i % 5) + 1, "리뷰" + i));
      }
      return null;
    });

    List<PopularBookRawData> results = readAll(Period.DAILY);

    assertThat(results).hasSize(100);
  }
}