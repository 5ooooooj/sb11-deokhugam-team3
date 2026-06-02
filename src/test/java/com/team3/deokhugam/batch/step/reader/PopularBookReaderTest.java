package com.team3.deokhugam.batch.step.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.team3.deokhugam.batch.dto.PopularBookRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.repository.review.ReviewRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.database.JpaPagingItemReader;
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
  private PlatformTransactionManager transactionManager;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private ReviewRepository reviewRepository;

  private PopularBookReader popularBookReader;

  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    popularBookReader = new PopularBookReader(entityManagerFactory);
    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @AfterEach
  void tearDown() {
    transactionTemplate.execute(status -> {
      reviewRepository.deleteAll();
      return null;
    });
  }

  @Test
  @DisplayName("성공: 기간 내 리뷰를 도서별로 집계")
  void read_daily_success() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    transactionTemplate.execute(status -> {
      Review review1 = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId, 4, "테스트 리뷰1"));
      Review review2 = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId, 2, "테스트 리뷰2"));
      Review oldReview = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId, 5, "오래된 리뷰"));

      entityManager.createQuery(
              "UPDATE Review r SET r.createdAt = :createdAt WHERE r.id = :id")
          .setParameter("createdAt", Instant.now().minus(2, ChronoUnit.DAYS))
          .setParameter("id", oldReview.getId())
          .executeUpdate();

      return null;
    });

    // when
    JpaPagingItemReader<PopularBookRawData> reader = popularBookReader.create(Period.DAILY);
    reader.open(new ExecutionContext());
    PopularBookRawData result = reader.read();

    // then
    assertThat(result).isNotNull();
    assertThat(result.bookId()).isEqualTo(bookId);
    assertThat(result.reviewCount()).isEqualTo(2);
    assertThat(result.ratingAvg()).isCloseTo(BigDecimal.valueOf(3.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 논리 삭제된 리뷰도 집계에 포함")
  void read_includeDeletedReview() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    transactionTemplate.execute(status -> {
      Review review = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId, 4, "테스트 리뷰"));
      Review deletedReview = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId, 2, "삭제된 리뷰"));

      entityManager.createQuery(
              "UPDATE Review r SET r.deletedAt = :deletedAt WHERE r.id = :id")
          .setParameter("deletedAt", Instant.now())
          .setParameter("id", deletedReview.getId())
          .executeUpdate();
      return null;
    });

    JpaPagingItemReader<PopularBookRawData> reader = popularBookReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    PopularBookRawData result = reader.read();

    // then
    assertThat(result).isNotNull();
    assertThat(result.reviewCount()).isEqualTo(2); // 삭제된 리뷰 포함
    assertThat(result.ratingAvg()).isCloseTo(BigDecimal.valueOf(3.0), within(BigDecimal.valueOf(0.001))); // (4+2)/2
  }

  @Test
  @DisplayName("성공: ALL_TIME은 전체 기간을 집계")
  void read_allTime_success() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    transactionTemplate.execute(status -> {
      Review review1 = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId, 5, "리뷰1"));
      Review review2 = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId, 3, "리뷰2"));
      Review review3 = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId, 4, "리뷰3"));

      entityManager.createQuery(
              "UPDATE Review r SET r.createdAt = :createdAt WHERE r.id = :id")
          .setParameter("createdAt", Instant.now().minus(365, ChronoUnit.DAYS))
          .setParameter("id", review1.getId())
          .executeUpdate();
      return null;
    });

    JpaPagingItemReader<PopularBookRawData> reader = popularBookReader.create(Period.ALL_TIME);
    reader.open(new ExecutionContext());

    // when
    PopularBookRawData result = reader.read();

    // then
    assertThat(result).isNotNull();
    assertThat(result.reviewCount()).isEqualTo(3); // 전체 포함
    assertThat(result.ratingAvg()).isCloseTo(BigDecimal.valueOf(4.0), within(BigDecimal.valueOf(0.001))); // (5+3+4)/3
  }

  @Test
  @DisplayName("성공: 기간 내 리뷰가 없으면 결과가 없음")
  void read_noReviewInPeriod() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    transactionTemplate.execute(status -> {
      Review oldReview = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId, 5, "오래된 리뷰"));

      entityManager.createQuery(
              "UPDATE Review r SET r.createdAt = :createdAt WHERE r.id = :id")
          .setParameter("createdAt", Instant.now().minus(2, ChronoUnit.DAYS))
          .setParameter("id", oldReview.getId())
          .executeUpdate();
      return null;
    });

    JpaPagingItemReader<PopularBookRawData> reader = popularBookReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    PopularBookRawData result = reader.read();

    // then
    assertThat(result).isNull();
  }

  // 도서 별 그룹핑 제대로 되는지 테스트

  @Test
  @DisplayName("성공: 여러 도서의 리뷰를 각각 집계")
  void read_multipleBooks() throws Exception {
    // given
    UUID bookId1 = UUID.randomUUID();
    UUID bookId2 = UUID.randomUUID();

    transactionTemplate.execute(status -> {
      reviewRepository.save(Review.create(UUID.randomUUID(), bookId1, 5, "리뷰1"));
      reviewRepository.save(Review.create(UUID.randomUUID(), bookId1, 3, "리뷰2"));
      reviewRepository.save(Review.create(UUID.randomUUID(), bookId2, 4, "리뷰3"));
      return null;
    });

    JpaPagingItemReader<PopularBookRawData> reader = popularBookReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    List<PopularBookRawData> results = new ArrayList<>();
    PopularBookRawData item;
    while ((item = reader.read()) != null) {
      results.add(item);
    }

    // then
    assertThat(results).hasSize(2);

    PopularBookRawData book1Result = results.stream()
        .filter(r -> r.bookId().equals(bookId1))
        .findFirst()
        .orElseThrow();
    assertThat(book1Result.reviewCount()).isEqualTo(2);
    assertThat(book1Result.ratingAvg()).isCloseTo(BigDecimal.valueOf(4.0), within(BigDecimal.valueOf(0.001))); // (5+3)/2
  }
}

