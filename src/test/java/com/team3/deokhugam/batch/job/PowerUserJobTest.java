package com.team3.deokhugam.batch.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewLike;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.repository.comment.CommentRepository;
import com.team3.deokhugam.repository.dashboard.PopularReviewRepository;
import com.team3.deokhugam.repository.dashboard.PowerUserRepository;
import com.team3.deokhugam.repository.review.ReviewLikeRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.test.JobLauncherTestUtils;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.batch.test.context.SpringBatchTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBatchTest
@SpringBootTest
@ActiveProfiles("test")
public class PowerUserJobTest {

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private JobRepositoryTestUtils jobRepositoryTestUtils;
  @Autowired private Job powerUserJob;
  @Autowired private PowerUserRepository powerUserRepository;
  @Autowired private PopularReviewRepository popularReviewRepository;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private ReviewLikeRepository reviewLikeRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private BookRepository bookRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;

  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(powerUserJob);
    transactionTemplate = new TransactionTemplate(transactionManager);
    jobRepositoryTestUtils.removeJobExecutions();
  }

  @AfterEach
  void tearDown() {
    powerUserRepository.deleteAll();
    popularReviewRepository.deleteAll();
    commentRepository.deleteAll();
    reviewLikeRepository.deleteAll();
    reviewRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();
  }

  private User saveUser() {
    return transactionTemplate.execute(status ->
        userRepository.save(
            new User("job-" + UUID.randomUUID() + "@test.com", "배치테스터", "Password1!")));
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

  // PopularReview 세팅
  private void savePopularReview(Review review, Period period, BigDecimal score) {
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

  private void addLikes(Review review, User liker) {
    transactionTemplate.execute(status -> {
      reviewLikeRepository.save(ReviewLike.create(review, liker));
      return null;
    });
  }

  private void addComment(Review review, User commenter) {
    transactionTemplate.execute(status -> {
      commentRepository.save(Comment.create(review, commenter, "댓글"));
      return null;
    });
  }

  @Test
  @DisplayName("성공: Job 실행 후 POWER_USERS에 기간별 결과가 저장")
  void job_success() throws Exception {
    // given
    User user = saveUser();
    Book book = saveBook();
    Review review = saveReview(user, book);

    savePopularReview(review, Period.DAILY, BigDecimal.valueOf(3.5));
    savePopularReview(review, Period.WEEKLY, BigDecimal.valueOf(3.5));
    savePopularReview(review, Period.MONTHLY, BigDecimal.valueOf(3.5));
    savePopularReview(review, Period.ALL_TIME, BigDecimal.valueOf(3.5));

    User liker = saveUser();
    addLikes(review, liker);
    addComment(review, liker);

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(powerUserRepository.findByPeriod(Period.DAILY)).isNotEmpty();
    assertThat(powerUserRepository.findByPeriod(Period.WEEKLY)).isNotEmpty();
    assertThat(powerUserRepository.findByPeriod(Period.MONTHLY)).isNotEmpty();
    assertThat(powerUserRepository.findByPeriod(Period.ALL_TIME)).isNotEmpty();
  }

  @Test
  @DisplayName("성공: Job 재실행 시 기존 데이터를 삭제하고 새로 저장")
  void job_rerun_success() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.DAILY, BigDecimal.valueOf(2.0));
    savePopularReview(review, Period.WEEKLY, BigDecimal.valueOf(2.0));
    savePopularReview(review, Period.MONTHLY, BigDecimal.valueOf(2.0));
    savePopularReview(review, Period.ALL_TIME, BigDecimal.valueOf(2.0));

    JobParameters params1 = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now().minusDays(1))
        .toJobParameters();
    jobLauncherTestUtils.launchJob(params1);

    long countAfterFirst = powerUserRepository.count();

    // when
    JobParameters params2 = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now())
        .toJobParameters();
    JobExecution execution = jobLauncherTestUtils.launchJob(params2);

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(powerUserRepository.count()).isEqualTo(countAfterFirst);
  }

  @Test
  @DisplayName("성공: 활동이 없으면 빈 결과로 Job이 완료")
  void job_noActivity_success() throws Exception {
    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(powerUserRepository.count()).isZero();
  }

  @Test
  @DisplayName("성공: 1위 유저의 rank가 1로 저장")
  void job_rankIsOne_forTopUser() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.DAILY, BigDecimal.valueOf(4.0));
    savePopularReview(review, Period.WEEKLY,  BigDecimal.valueOf(4.0));
    savePopularReview(review, Period.MONTHLY,  BigDecimal.valueOf(4.0));
    savePopularReview(review, Period.ALL_TIME,  BigDecimal.valueOf(4.0));

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // then
    List<PowerUser> dailyResults = powerUserRepository.findByPeriod(Period.DAILY);
    assertThat(dailyResults).hasSize(1);
    assertThat(dailyResults.get(0).getRanking()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 다수 유저 있을 때 점수 기반 순위가 올바르게 저장됨")
  void job_globalRankIsCorrect_forMultipleUsers() throws Exception {
    // given
    User user1 = saveUser();
    User user2 = saveUser();
    User user3 = saveUser();

    Book book = saveBook();
    Review review1 = saveReview(user1, book);
    Review review2 = saveReview(user2, saveBook());
    Review review3 = saveReview(user3, saveBook());

    savePopularReview(review1, Period.DAILY,  BigDecimal.valueOf(4.0));
    savePopularReview(review1, Period.WEEKLY,  BigDecimal.valueOf(4.0));
    savePopularReview(review1, Period.MONTHLY,  BigDecimal.valueOf(4.0));
    savePopularReview(review1, Period.ALL_TIME,  BigDecimal.valueOf(4.0));

    savePopularReview(review2, Period.DAILY,  BigDecimal.valueOf(2.0));
    savePopularReview(review2, Period.WEEKLY, BigDecimal.valueOf(2.0));
    savePopularReview(review2, Period.MONTHLY, BigDecimal.valueOf(2.0));
    savePopularReview(review2, Period.ALL_TIME, BigDecimal.valueOf(2.0));

    // user1이 review2에 좋아요+댓글, user2가 review1에 좋아요
    addLikes(review2, user1);
    addComment(review2, user1);
    addLikes(review1, user2);
    addComment(review1, user3);

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // then
    List<PowerUser> results = powerUserRepository.findByPeriodOrderByScoreDesc(Period.DAILY);
    assertThat(results).hasSizeGreaterThanOrEqualTo(3);
    assertThat(results.get(0).getUserId()).isEqualTo(user1.getId());
    assertThat(results.get(0).getRanking()).isEqualTo(1);
    assertThat(results.get(1).getUserId()).isEqualTo(user2.getId());
    assertThat(results.get(1).getRanking()).isEqualTo(2);
  }

  @Test
  @DisplayName("성공: 논리 삭제된 리뷰도 파워 유저 점수 계산에 포함")
  void job_includesDeletedReviewAuthor() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.DAILY, BigDecimal.valueOf(3.0));
    savePopularReview(review, Period.WEEKLY, BigDecimal.valueOf(3.0));
    savePopularReview(review, Period.MONTHLY, BigDecimal.valueOf(3.0));
    savePopularReview(review, Period.ALL_TIME, BigDecimal.valueOf(3.0));

    transactionTemplate.execute(status -> {
      entityManager.createQuery(
              "UPDATE Review r SET r.deletedAt = :deletedAt WHERE r.id = :id")
          .setParameter("deletedAt", Instant.now())
          .setParameter("id", review.getId())
          .executeUpdate();
      return null;
    });

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    // Reader 쿼리에 Review.deletedAt 필터 없으므로 논리 삭제 리뷰도 포함
    assertThat(powerUserRepository.findByPeriod(Period.DAILY)).hasSize(1);
  }
}