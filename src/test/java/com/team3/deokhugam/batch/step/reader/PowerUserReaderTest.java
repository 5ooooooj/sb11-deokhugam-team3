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
class PowerUserReaderTest {

  @Autowired
  private PlatformTransactionManager transactionManager;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private ReviewLikeRepository reviewLikeRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private PopularReviewRepository popularReviewRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private UserRepository userRepository;

  private PowerUserReader powerUserReader;
  private TransactionTemplate transactionTemplate;

  // 한국 시간(KST) 기준의 '어제' 시간대 정의
  private final Instant kstYesterday = LocalDateTime.now(ZoneId.of("Asia/Seoul"))
      .minusDays(1)
      .atZone(ZoneId.of("Asia/Seoul"))
      .toInstant();

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

  // calculated_at 시간을 외부에서 주입할 수 있도록 변경 (기존: now() 하드코딩)
  private void savePopularReview(Review review, Period period, double score, Instant calculatedAt) {
    transactionTemplate.execute(status -> {
      entityManager.createNativeQuery(
              "INSERT INTO popular_reviews (id, review_id, period, score, ranking, like_count, comment_count, calculated_at) "
                  + "VALUES (gen_random_uuid(), :reviewId, :period, :score, 1, 0, 0, :calculatedAt)")
          .setParameter("reviewId", review.getId())
          .setParameter("period", period.name())
          .setParameter("score", score)
          .setParameter("calculatedAt", calculatedAt)
          .executeUpdate();
      return null;
    });
  }

  private List<PowerUserRawData> readAll(Period period) throws Exception {
    AbstractItemStreamItemReader<PowerUserRawData> reader = powerUserReader.create(period);
    reader.open(new ExecutionContext());

    List<PowerUserRawData> results = new ArrayList<>();
    PowerUserRawData item;
    while ((item = reader.read()) != null) {
      results.add(item);
    }

    reader.close();
    return results;
  }

  private void saveLike(Review review, User user, Instant createdAt) {
    transactionTemplate.execute(status -> {
      ReviewLike like = reviewLikeRepository.save(ReviewLike.create(review, user));
      entityManager.createQuery(
              "UPDATE ReviewLike rl SET rl.createdAt = :createdAt WHERE rl.id = :id")
          .setParameter("createdAt", createdAt)
          .setParameter("id", like.getId())
          .executeUpdate();
      return null;
    });
  }

  @Test
  @DisplayName("성공: 기간 내 활동을 유저별로 집계")
  void read_daily_success() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    // 인기 리뷰의 시간대를 '어제' 시간대로 고정
    savePopularReview(review, Period.DAILY, 3.0, kstYesterday.plus(1, ChronoUnit.HOURS));

    transactionTemplate.execute(status -> {
      ReviewLike like = reviewLikeRepository.save(ReviewLike.create(review, user));
      Comment comment = commentRepository.save(Comment.create(review, user, "댓글"));

      // 새로 생성된 라이크와 코멘트도 '어제' 시간대 내부로 강제 패치
      entityManager.createQuery("UPDATE ReviewLike rl SET rl.createdAt = :createdAt WHERE rl.id = :id")
          .setParameter("createdAt", kstYesterday.plus(1, ChronoUnit.HOURS))
          .setParameter("id", like.getId())
          .executeUpdate();

      entityManager.createQuery("UPDATE Comment c SET c.createdAt = :createdAt WHERE c.id = :id")
          .setParameter("createdAt", kstYesterday.plus(2, ChronoUnit.HOURS))
          .setParameter("id", comment.getId())
          .executeUpdate();

      return null;
    });

    List<PowerUserRawData> results = readAll(Period.DAILY);

    assertThat(results).isNotEmpty();
    PowerUserRawData userResult = results.stream()
        .filter(r -> r.userId().equals(user.getId()))
        .findFirst().orElseThrow();
    assertThat(userResult.reviewScoreSum())
        .isCloseTo(BigDecimal.valueOf(3.0), within(BigDecimal.valueOf(0.001)));
    assertThat(userResult.score())
        .isCloseTo(BigDecimal.valueOf(2.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 논리 삭제된 유저는 집계에서 제외")
  void read_excludesDeletedUser() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.DAILY, 3.0, kstYesterday.plus(1, ChronoUnit.HOURS));

    transactionTemplate.execute(status -> {
      entityManager.createQuery(
              "UPDATE User u SET u.deletedAt = :deletedAt WHERE u.id = :id")
          .setParameter("deletedAt", Instant.now())
          .setParameter("id", user.getId())
          .executeUpdate();
      return null;
    });

    List<PowerUserRawData> results = readAll(Period.DAILY);

    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("성공: ALL_TIME은 전체 기간을 집계")
  void read_allTime_success() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    // ALL_TIME 집계는 기간 조건이 없으므로 임의의 현재 시간대를 주입해도 무방
    savePopularReview(review, Period.ALL_TIME, 5.0, Instant.now());

    transactionTemplate.execute(status -> {
      ReviewLike like = reviewLikeRepository.save(ReviewLike.create(review, user));

      // 오래된 과거 데이터 세팅
      entityManager.createQuery(
              "UPDATE ReviewLike rl SET rl.createdAt = :createdAt WHERE rl.id = :id")
          .setParameter("createdAt", Instant.now().minus(200, ChronoUnit.DAYS))
          .setParameter("id", like.getId())
          .executeUpdate();
      return null;
    });

    List<PowerUserRawData> results = readAll(Period.ALL_TIME);

    assertThat(results).isNotEmpty();
    PowerUserRawData userResult = results.stream()
        .filter(r -> r.userId().equals(user.getId()))
        .findFirst().orElseThrow();
    assertThat(userResult.reviewScoreSum())
        .isCloseTo(BigDecimal.valueOf(5.0), within(BigDecimal.valueOf(0.001)));
    assertThat(userResult.score())
        .isCloseTo(BigDecimal.valueOf(2.7), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 활동이 없으면 결과가 없음")
  void read_noActivity() throws Exception {
    saveUser();

    List<PowerUserRawData> results = readAll(Period.DAILY);

    assertThat(results).isEmpty();
  }

  @Test
  @DisplayName("성공: 여러 유저를 각각 집계")
  void read_multipleUsers() throws Exception {
    User user1 = saveUser();
    User user2 = saveUser();
    Review review1 = saveReview(user1, saveBook());
    Review review2 = saveReview(user2, saveBook());

    // 다중 유저 인기리뷰들의 집계 시점을 '어제' 범위로 일괄 패치
    savePopularReview(review1, Period.DAILY, 4.0, kstYesterday.plus(1, ChronoUnit.HOURS));
    savePopularReview(review2, Period.DAILY, 2.0, kstYesterday.plus(2, ChronoUnit.HOURS));

    List<PowerUserRawData> results = readAll(Period.DAILY);

    assertThat(results).hasSize(2);

    PowerUserRawData user1Result = results.stream()
        .filter(r -> r.userId().equals(user1.getId()))
        .findFirst().orElseThrow();
    assertThat(user1Result.reviewScoreSum())
        .isCloseTo(BigDecimal.valueOf(4.0), within(BigDecimal.valueOf(0.001)));
    assertThat(user1Result.score())
        .isCloseTo(BigDecimal.valueOf(2.0), within(BigDecimal.valueOf(0.001)));

    PowerUserRawData user2Result = results.stream()
        .filter(r -> r.userId().equals(user2.getId()))
        .findFirst().orElseThrow();
    assertThat(user2Result.reviewScoreSum())
        .isCloseTo(BigDecimal.valueOf(2.0), within(BigDecimal.valueOf(0.001)));
    assertThat(user2Result.score())
        .isCloseTo(BigDecimal.valueOf(1.0), within(BigDecimal.valueOf(0.001)));
  }

  @Test
  @DisplayName("성공: 결과가 점수 내림차순으로 정렬")
  void read_orderedByScoreDesc() throws Exception {
    User user1 = saveUser();
    User user2 = saveUser();
    Review review1 = saveReview(user1, saveBook());
    Review review2 = saveReview(user2, saveBook());

    // 정렬 검증 대상들의 집계 시점도 '어제' 내부 시간으로 명시
    savePopularReview(review1, Period.DAILY, 2.0, kstYesterday.plus(1, ChronoUnit.HOURS));
    savePopularReview(review2, Period.DAILY, 4.0, kstYesterday.plus(2, ChronoUnit.HOURS));

    List<PowerUserRawData> results = readAll(Period.DAILY);

    assertThat(results).hasSize(2);
    assertThat(results.get(0).userId()).isEqualTo(user2.getId());
    assertThat(results.get(1).userId()).isEqualTo(user1.getId());
  }

  @Test
  @DisplayName("성공: MAX_ITEM_COUNT 초과 시 상위 100개만 반환")
  void read_maxItemCount() throws Exception {
    transactionTemplate.execute(status -> {
      for (int i = 0; i < 105; i++) {
        User user = userRepository.save(
            new User("user" + i + "-" + UUID.randomUUID() + "@test.com", "유저" + i, "Password1!"));
        Book book = bookRepository.save(new Book(
            UUID.randomUUID(), "도서" + i, "저자", "설명",
            "출판사", LocalDate.of(2026, 1, 1), null, null));
        Review review = reviewRepository.save(Review.create(user, book, 5, "리뷰" + i));

        // 루프 내 벌크성 데이터 삽입 시에도 명확하게 '어제' 타임스탬프를 부여
        entityManager.createNativeQuery(
                "INSERT INTO popular_reviews (id, review_id, period, score, ranking, like_count, comment_count, calculated_at) "
                    + "VALUES (gen_random_uuid(), :reviewId, :period, :score, 1, 0, 0, :calculatedAt)")
            .setParameter("reviewId", review.getId())
            .setParameter("period", Period.DAILY.name())
            .setParameter("score", i + 1.0)
            .setParameter("calculatedAt", kstYesterday.plus(2, ChronoUnit.HOURS))
            .executeUpdate();
      }
      return null;
    });

    List<PowerUserRawData> results = readAll(Period.DAILY);

    assertThat(results).hasSize(100);
  }

  @Test
  @DisplayName("성공: 동점 유저는 오래된 유저가 우선 정렬")
  void read_sameScore_orderedByCreatedAtAsc() throws Exception {
    // given
    User newUser = saveUser();   // 가장 최신 유저
    User midUser = saveUser();   // 중간
    User oldUser = saveUser();   // 가장 오래된 유저

    // createdAt 강제 세팅
    transactionTemplate.execute(status -> {
      entityManager.createQuery("UPDATE User u SET u.createdAt = :createdAt WHERE u.id = :id")
          .setParameter("createdAt", Instant.now().minus(1, ChronoUnit.DAYS))
          .setParameter("id", newUser.getId())
          .executeUpdate();
      entityManager.createQuery("UPDATE User u SET u.createdAt = :createdAt WHERE u.id = :id")
          .setParameter("createdAt", Instant.now().minus(5, ChronoUnit.DAYS))
          .setParameter("id", midUser.getId())
          .executeUpdate();
      entityManager.createQuery("UPDATE User u SET u.createdAt = :createdAt WHERE u.id = :id")
          .setParameter("createdAt", Instant.now().minus(10, ChronoUnit.DAYS))
          .setParameter("id", oldUser.getId())
          .executeUpdate();
      return null;
    });

    // 세 유저 모두 동일한 좋아요 2개 → score = 2*0.2 = 0.4 동점
    Review review1 = saveReview(saveUser(), saveBook());
    Review review2 = saveReview(saveUser(), saveBook());

    saveLike(review1, newUser, kstYesterday.plus(1, ChronoUnit.HOURS));
    saveLike(review2, newUser, kstYesterday.plus(2, ChronoUnit.HOURS));

    saveLike(review1, midUser, kstYesterday.plus(3, ChronoUnit.HOURS));
    saveLike(review2, midUser, kstYesterday.plus(4, ChronoUnit.HOURS));

    saveLike(review1, oldUser, kstYesterday.plus(5, ChronoUnit.HOURS));
    saveLike(review2, oldUser, kstYesterday.plus(6, ChronoUnit.HOURS));

    // when
    List<PowerUserRawData> results = readAll(Period.DAILY);

    // then
    assertThat(results).hasSize(3);
    // 동점이면 오래된 유저 우선 → oldUser > midUser > newUser
    assertThat(results.get(0).userId()).isEqualTo(oldUser.getId());
    assertThat(results.get(1).userId()).isEqualTo(midUser.getId());
    assertThat(results.get(2).userId()).isEqualTo(newUser.getId());
  }
}