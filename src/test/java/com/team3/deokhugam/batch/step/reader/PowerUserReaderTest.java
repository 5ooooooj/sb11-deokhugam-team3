package com.team3.deokhugam.batch.step.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import com.team3.deokhugam.batch.dto.PowerUserRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewLike;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.repository.comment.CommentRepository;
import com.team3.deokhugam.repository.dashboard.PopularReviewRepository;
import com.team3.deokhugam.repository.review.ReviewLikeRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
class PowerUserReaderTest {

  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private EntityManager entityManager;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private ReviewLikeRepository reviewLikeRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private PopularReviewRepository popularReviewRepository;
  @Autowired private BookRepository bookRepository;
  @Autowired private UserRepository userRepository;

  private PowerUserReader powerUserReader;
  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    powerUserReader = new PowerUserReader(entityManagerFactory);
    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @AfterEach
  void tearDown() {
    transactionTemplate.execute(status -> {
      popularReviewRepository.deleteAll();
      commentRepository.deleteAll();
      reviewLikeRepository.deleteAll();
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

  private Review saveReview(User user, Book book) {
    return transactionTemplate.execute(status ->
        reviewRepository.save(Review.create(user, book, 5, "테스트 리뷰")));
  }

  private void savePopularReview(Review review, Period period, double score) {
    transactionTemplate.execute(status -> {
      entityManager.createNativeQuery(
              "INSERT INTO popular_reviews (id, review_id, period, score, ranking, like_count, comment_count, calculated_at) "
                  + "VALUES (gen_random_uuid(), :reviewId, :period, :score, 1, 0, 0, now())")
          .setParameter("reviewId", review.getId())
          .setParameter("period", period.name())
          .setParameter("score", score)
          .executeUpdate();
      return null;
    });
  }

  @Test
  @DisplayName("성공: 기간 내 활동을 유저별로 집계")
  void read_daily_success() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.DAILY, 3.0);

    transactionTemplate.execute(status -> {
      User liker = userRepository.save(
          new User("liker-" + UUID.randomUUID() + "@test.com", "좋아요", "Password1!"));
      User commenter = userRepository.save(
          new User("commenter-" + UUID.randomUUID() + "@test.com", "댓글러", "Password1!"));

      reviewLikeRepository.save(ReviewLike.create(review, liker));
      commentRepository.save(Comment.create(review, commenter, "댓글"));
      return null;
    });

    // when
    JpaPagingItemReader<PowerUserRawData> reader = powerUserReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // then
    List<PowerUserRawData> results = new ArrayList<>();
    PowerUserRawData item;
    while ((item = reader.read()) != null) {
      results.add(item);
    }

    assertThat(results).isNotEmpty();
    PowerUserRawData userResult = results.stream()
        .filter(r -> r.userId().equals(user.getId()))
        .findFirst().orElseThrow();
    assertThat(userResult.reviewScoreSum()).isCloseTo(BigDecimal.valueOf(3.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 논리 삭제된 유저는 집계에서 제외")
  void read_excludesDeletedUser() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.DAILY, 3.0);

    transactionTemplate.execute(status -> {
      entityManager.createQuery(
              "UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :id")
          .setParameter("deletedAt", Instant.now())
          .setParameter("id", user.getId())
          .executeUpdate();
      return null;
    });

    JpaPagingItemReader<PowerUserRawData> reader = powerUserReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    PowerUserRawData result = reader.read();

    // then
    assertThat(result).isNull(); // 논리 삭제 유저 제외
  }

  @Test
  @DisplayName("성공: ALL_TIME은 전체 기간을 집계")
  void read_allTime_success() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.ALL_TIME, 5.0);

    transactionTemplate.execute(status -> {
      User liker = userRepository.save(
          new User("liker-" + UUID.randomUUID() + "@test.com", "좋아요", "Password1!"));
      ReviewLike like = reviewLikeRepository.save(ReviewLike.create(review, liker));

      entityManager.createQuery(
              "UPDATE ReviewLike rl SET rl.createdAt = :createdAt WHERE rl.id = :id")
          .setParameter("createdAt", Instant.now().minus(200, ChronoUnit.DAYS))
          .setParameter("id", like.getId())
          .executeUpdate();
      return null;
    });

    JpaPagingItemReader<PowerUserRawData> reader = powerUserReader.create(Period.ALL_TIME);
    reader.open(new ExecutionContext());

    // when
    List<PowerUserRawData> results = new ArrayList<>();
    PowerUserRawData item;
    while ((item = reader.read()) != null) {
      results.add(item);
    }

    // then
    assertThat(results).isNotEmpty();
    PowerUserRawData userResult = results.stream()
        .filter(r -> r.userId().equals(user.getId()))
        .findFirst().orElseThrow();
    assertThat(userResult.reviewScoreSum()).isCloseTo(BigDecimal.valueOf(5.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 활동이 없으면 결과가 없음")
  void read_noActivity() throws Exception {
    // given - 유저만 있고 활동 없음
    saveUser();

    JpaPagingItemReader<PowerUserRawData> reader = powerUserReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    PowerUserRawData result = reader.read();

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("성공: 여러 유저를 각각 집계")
  void read_multipleUsers() throws Exception {
    // given
    User user1 = saveUser();
    User user2 = saveUser();
    Review review1 = saveReview(user1, saveBook());
    Review review2 = saveReview(user2, saveBook());

    savePopularReview(review1, Period.DAILY, 4.0);
    savePopularReview(review2, Period.DAILY, 2.0);

    JpaPagingItemReader<PowerUserRawData> reader = powerUserReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    List<PowerUserRawData> results = new ArrayList<>();
    PowerUserRawData item;
    while ((item = reader.read()) != null) {
      results.add(item);
    }

    // then
    assertThat(results).hasSize(2);

    PowerUserRawData user1Result = results.stream()
        .filter(r -> r.userId().equals(user1.getId()))
        .findFirst().orElseThrow();
    assertThat(user1Result.reviewScoreSum())
        .isCloseTo(BigDecimal.valueOf(4.0), within(BigDecimal.valueOf(0.001)));

    PowerUserRawData user2Result = results.stream()
        .filter(r -> r.userId().equals(user2.getId()))
        .findFirst().orElseThrow();
    assertThat(user2Result.reviewScoreSum())
        .isCloseTo(BigDecimal.valueOf(2.0), within(BigDecimal.valueOf(0.001)));
  }
}