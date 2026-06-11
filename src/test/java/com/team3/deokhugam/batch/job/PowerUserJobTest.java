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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.support.TaskExecutorJobLauncher;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.test.JobRepositoryTestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
@ActiveProfiles("test")
public class PowerUserJobTest {

  @Autowired
  private
  JobRepository jobRepository;

  @Autowired
  @Qualifier("powerUserDailyJob")
  private Job powerUserDailyJob;

  @Autowired
  @Qualifier("powerUserWeeklyJob")
  private Job powerUserWeeklyJob;

  @Autowired
  @Qualifier("powerUserMonthlyJob")
  private Job powerUserMonthlyJob;

  @Autowired
  @Qualifier("powerUserAllTimeJob")
  private Job powerUserAllTimeJob;

  @Autowired
  private PowerUserRepository powerUserRepository;

  @Autowired
  private PopularReviewRepository popularReviewRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private ReviewLikeRepository reviewLikeRepository;

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private BookRepository bookRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PlatformTransactionManager transactionManager;

  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    transactionTemplate = new TransactionTemplate(transactionManager);
    new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
    powerUserRepository.deleteAll();
    popularReviewRepository.deleteAll();
    commentRepository.deleteAll();
    reviewLikeRepository.deleteAll();
    reviewRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();
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

  private JobExecution launchJob(Job job) throws Exception {
    TaskExecutorJobLauncher syncLauncher = new TaskExecutorJobLauncher();
    syncLauncher.setJobRepository(jobRepository);
    syncLauncher.setTaskExecutor(new SyncTaskExecutor());
    syncLauncher.afterPropertiesSet();

    return syncLauncher.run(job, new JobParametersBuilder()
        .addLocalDateTime("runAt", LocalDateTime.now())
        .addLong("nonce", System.nanoTime())
        .toJobParameters());
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
  @DisplayName("성공: 각 Period Job 실행 후 기간별 결과가 저장")
  void job_success() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());

    savePopularReview(review, Period.DAILY, BigDecimal.valueOf(3.5));
    savePopularReview(review, Period.WEEKLY, BigDecimal.valueOf(3.5));
    savePopularReview(review, Period.MONTHLY, BigDecimal.valueOf(3.5));
    savePopularReview(review, Period.ALL_TIME, BigDecimal.valueOf(3.5));

    User liker = saveUser();
    addLikes(review, liker);
    addComment(review, liker);

    launchJob(powerUserDailyJob);
    launchJob(powerUserWeeklyJob);
    launchJob(powerUserMonthlyJob);
    JobExecution allTimeExecution = launchJob(powerUserAllTimeJob);

    assertThat(allTimeExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(powerUserRepository.findByPeriod(Period.DAILY)).isNotEmpty();
    assertThat(powerUserRepository.findByPeriod(Period.WEEKLY)).isNotEmpty();
    assertThat(powerUserRepository.findByPeriod(Period.MONTHLY)).isNotEmpty();
    assertThat(powerUserRepository.findByPeriod(Period.ALL_TIME)).isNotEmpty();
  }

  @Test
  @DisplayName("성공: Job 재실행 시 기존 데이터를 삭제하고 새로 저장")
  void job_rerun_success() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.DAILY, BigDecimal.valueOf(2.0));

    launchJob(powerUserDailyJob);
    long countAfterFirst = powerUserRepository.findByPeriod(Period.DAILY).size();

    JobExecution execution = launchJob(powerUserDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(powerUserRepository.findByPeriod(Period.DAILY)).hasSize((int) countAfterFirst);
  }

  @Test
  @DisplayName("성공: 활동이 없으면 빈 결과로 Job이 완료")
  void job_noActivity_success() throws Exception {
    JobExecution execution = launchJob(powerUserDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(powerUserRepository.findByPeriod(Period.DAILY)).isEmpty();
  }

  @Test
  @DisplayName("성공: 1위 유저의 rank가 1로 저장")
  void job_rankIsOne_forTopUser() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.DAILY, BigDecimal.valueOf(4.0));

    JobExecution execution = launchJob(powerUserDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PowerUser> dailyResults = powerUserRepository.findByPeriod(Period.DAILY);
    assertThat(dailyResults).hasSize(1);
    assertThat(dailyResults.get(0).getRanking()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 다수 유저 있을 때 점수 기반 순위가 올바르게 저장됨")
  void job_globalRankIsCorrect_forMultipleUsers() throws Exception {
    User user1 = saveUser();
    User user2 = saveUser();
    User user3 = saveUser();

    Review review1 = saveReview(user1, saveBook());
    Review review2 = saveReview(user2, saveBook());

    savePopularReview(review1, Period.DAILY, BigDecimal.valueOf(4.0));
    savePopularReview(review2, Period.DAILY, BigDecimal.valueOf(2.0));

    addLikes(review2, user1);
    addComment(review2, user1);
    addLikes(review1, user2);
    addComment(review1, user3);

    JobExecution execution = launchJob(powerUserDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PowerUser> results = powerUserRepository.findByPeriodOrderByScoreDesc(Period.DAILY);
    assertThat(results).hasSizeGreaterThanOrEqualTo(3);
    assertThat(results.get(0).getUserId()).isEqualTo(user1.getId());
    assertThat(results.get(0).getRanking()).isEqualTo(1);
    assertThat(results.get(1).getUserId()).isEqualTo(user2.getId());
    assertThat(results.get(1).getRanking()).isEqualTo(2);
  }

  @Test
  @DisplayName("성공: 동점 유저는 같은 rank 부여 (1, 1, 3)")
  void job_sameScore_sameRank() throws Exception {
    User user1 = saveUser();
    User user2 = saveUser();
    User user3 = saveUser();

    Review review1 = saveReview(user1, saveBook());
    Review review2 = saveReview(user2, saveBook());
    Review review3 = saveReview(user3, saveBook());

    savePopularReview(review1, Period.DAILY, BigDecimal.valueOf(2.0));
    savePopularReview(review2, Period.DAILY, BigDecimal.valueOf(2.0));
    savePopularReview(review3, Period.DAILY, BigDecimal.valueOf(1.0));

    JobExecution execution = launchJob(powerUserDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PowerUser> results = powerUserRepository.findByPeriodOrderByScoreDesc(Period.DAILY);
    assertThat(results).hasSize(3);
    assertThat(results.stream().filter(r -> r.getRanking() == 1).count()).isEqualTo(2);
    assertThat(results.stream().filter(r -> r.getRanking() == 3).count()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 논리 삭제된 리뷰도 파워 유저 점수 계산에 포함")
  void job_includesDeletedReviewAuthor() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    savePopularReview(review, Period.DAILY, BigDecimal.valueOf(3.0));

    transactionTemplate.execute(status -> {
      entityManager.createQuery(
              "UPDATE Review r SET r.deletedAt = :deletedAt WHERE r.id = :id")
          .setParameter("deletedAt", Instant.now())
          .setParameter("id", review.getId())
          .executeUpdate();
      return null;
    });

    JobExecution execution = launchJob(powerUserDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(powerUserRepository.findByPeriod(Period.DAILY)).hasSize(1);
  }

  @Test
  @DisplayName("성공: 기존 데이터 있을 때 활동 0건이면 stale 데이터 삭제")
  void job_staleDataCleared_whenNoActivity() throws Exception {
    User user = saveUser();
    transactionTemplate.execute(status -> {
      PowerUser stale = PowerUser.builder()
          .userId(user.getId())
          .period(Period.DAILY)
          .score(BigDecimal.valueOf(1.0))
          .reviewScoreSum(BigDecimal.ZERO)
          .likeCount(0)
          .commentCount(0)
          .calculatedAt(Instant.now())
          .build();
      powerUserRepository.save(stale);
      return null;
    });

    JobExecution execution = launchJob(powerUserDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(powerUserRepository.findByPeriod(Period.DAILY)).isEmpty();
  }
}