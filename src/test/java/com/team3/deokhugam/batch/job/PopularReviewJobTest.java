package com.team3.deokhugam.batch.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.dashboard.PopularReview;
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
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
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
public class PopularReviewJobTest {

  @Autowired
  private JobRepository jobRepository;

  @Autowired
  @Qualifier("popularReviewDailyJob")
  private Job popularReviewDailyJob;

  @Autowired
  @Qualifier("popularReviewWeeklyJob")
  private Job popularReviewWeeklyJob;

  @Autowired
  @Qualifier("popularReviewMonthlyJob")
  private Job popularReviewMonthlyJob;

  @Autowired
  @Qualifier("popularReviewAllTimeJob")
  private Job popularReviewAllTimeJob;

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
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  // 한국 시간(KST) 기준의 '어제' 시간대 정의
  private final Instant kstYesterday = LocalDateTime.now(KST)
      .minusDays(1)
      .atZone(KST)
      .toInstant();

  @BeforeEach
  void setUp() {
    transactionTemplate = new TransactionTemplate(transactionManager);
    new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
    popularReviewRepository.deleteAll();
    commentRepository.deleteAll();
    reviewLikeRepository.deleteAll();
    reviewRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();
  }

  @AfterEach
  void tearDown() {
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
        .addLocalDate("targetDate", LocalDate.now(KST)) // 💡 현행화 유지
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

  // 하드코딩된 시간 대신 명시적인 날짜를 주입받아 등록하도록 변경
  private void addLikesWithDate(Review review, int count, Instant createdAt) {
    transactionTemplate.execute(status -> {
      for (int i = 0; i < count; i++) {
        User liker = userRepository.save(
            new User("liker-" + UUID.randomUUID() + "@test.com", "좋아요유저", "Password1!"));
        ReviewLike like = reviewLikeRepository.save(ReviewLike.create(review, liker));
        entityManager.createQuery(
                "UPDATE ReviewLike rl SET rl.createdAt = :createdAt WHERE rl.id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", like.getId())
            .executeUpdate();
      }
      return null;
    });
  }

  private void addCommentsWithDate(Review review, int count, Instant createdAt) {
    transactionTemplate.execute(status -> {
      for (int i = 0; i < count; i++) {
        User commenter = userRepository.save(
            new User("commenter-" + UUID.randomUUID() + "@test.com", "댓글유저", "Password1!"));
        Comment comment = commentRepository.save(Comment.create(review, commenter, "댓글" + i));
        entityManager.createQuery(
                "UPDATE Comment c SET c.createdAt = :createdAt WHERE c.id = :id")
            .setParameter("createdAt", createdAt)
            .setParameter("id", comment.getId())
            .executeUpdate();
      }
      return null;
    });
  }

  @Test
  @DisplayName("성공: 각 Period Job 실행 후 기간별 결과가 저장")
  void job_success() throws Exception {
    User user = saveUser();

    // Daily 집계 대상 (어제 범위 생성)
    Review dailyReview = saveReview(user, saveBook());
    addLikesWithDate(dailyReview, 2, kstYesterday.plus(1, ChronoUnit.HOURS));
    addCommentsWithDate(dailyReview, 1, kstYesterday.plus(2, ChronoUnit.HOURS));

    // Weekly 집계 대상 (3일 전 생성 -> Daily 누락, Weekly 포착)
    Review weeklyReview = saveReview(user, saveBook());
    addLikesWithDate(weeklyReview, 1, kstYesterday.minus(3, ChronoUnit.DAYS));

    // Monthly 집계 대상 (15일 전 생성 -> Daily, Weekly 누락, Monthly 포착)
    Review monthlyReview = saveReview(user, saveBook());
    addCommentsWithDate(monthlyReview, 2, kstYesterday.minus(15, ChronoUnit.DAYS));

    // All Time 집계 대상 (200일 전 생성 -> All Time에만 잡힘)
    Review allTimeReview = saveReview(user, saveBook());
    addLikesWithDate(allTimeReview, 3, kstYesterday.minus(200, ChronoUnit.DAYS));

    launchJob(popularReviewDailyJob);
    launchJob(popularReviewWeeklyJob);
    launchJob(popularReviewMonthlyJob);
    JobExecution allTimeExecution = launchJob(popularReviewAllTimeJob);

    assertThat(allTimeExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // 누적 데이터 산출 범위 구조 검증
    assertThat(popularReviewRepository.findByPeriod(Period.DAILY)).hasSize(1);
    assertThat(popularReviewRepository.findByPeriod(Period.WEEKLY)).hasSize(2);
    assertThat(popularReviewRepository.findByPeriod(Period.MONTHLY)).hasSize(3);
    assertThat(popularReviewRepository.findByPeriod(Period.ALL_TIME)).hasSize(4);
  }

  @Test
  @DisplayName("성공: Job 재실행 시 기존 데이터를 삭제하고 새로 저장")
  void job_rerun_success() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    addLikesWithDate(review, 1, kstYesterday.plus(1, ChronoUnit.HOURS));

    launchJob(popularReviewDailyJob);
    long countAfterFirst = popularReviewRepository.findByPeriod(Period.DAILY).size();

    JobExecution execution = launchJob(popularReviewDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularReviewRepository.findByPeriod(Period.DAILY)).hasSize((int) countAfterFirst);
  }

  @Test
  @DisplayName("성공: 리뷰가 없으면 빈 결과로 Job이 완료")
  void job_noReview_success() throws Exception {
    JobExecution execution = launchJob(popularReviewDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularReviewRepository.findByPeriod(Period.DAILY)).isEmpty();
  }

  @Test
  @DisplayName("성공: 1위 리뷰의 rank가 1로 저장")
  void job_rankIsOne_forTopReview() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    addLikesWithDate(review, 3, kstYesterday.plus(1, ChronoUnit.HOURS));
    addCommentsWithDate(review, 2, kstYesterday.plus(2, ChronoUnit.HOURS));

    JobExecution execution = launchJob(popularReviewDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PopularReview> dailyResults = popularReviewRepository.findByPeriod(Period.DAILY);
    assertThat(dailyResults).hasSize(1);
    assertThat(dailyResults.get(0).getRanking()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 다수 리뷰 있을 때 점수 기반 순위가 올바르게 저장됨")
  void job_globalRankIsCorrect_forMultipleReviews() throws Exception {
    User user = saveUser();
    Instant yesterday = kstYesterday;

    Review review1 = saveReview(user, saveBook());
    addLikesWithDate(review1, 3, yesterday.plus(1, ChronoUnit.HOURS));
    addCommentsWithDate(review1, 5, yesterday.plus(2, ChronoUnit.HOURS));

    Review review2 = saveReview(user, saveBook());
    addLikesWithDate(review2, 5, yesterday.plus(1, ChronoUnit.HOURS));
    addCommentsWithDate(review2, 2, yesterday.plus(2, ChronoUnit.HOURS));

    Review review3 = saveReview(user, saveBook());
    addLikesWithDate(review3, 1, yesterday.plus(1, ChronoUnit.HOURS));

    JobExecution execution = launchJob(popularReviewDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PopularReview> results = popularReviewRepository.findByPeriodOrderByScoreDesc(Period.DAILY);
    assertThat(results).hasSize(3);
    assertThat(results.get(0).getReviewId()).isEqualTo(review1.getId());
    assertThat(results.get(0).getRanking()).isEqualTo(1);
    assertThat(results.get(1).getReviewId()).isEqualTo(review2.getId());
    assertThat(results.get(1).getRanking()).isEqualTo(2);
    assertThat(results.get(2).getReviewId()).isEqualTo(review3.getId());
    assertThat(results.get(2).getRanking()).isEqualTo(3);
  }

  @Test
  @DisplayName("성공: 동점 리뷰는 같은 rank 부여 (1, 1, 3)")
  void job_sameScore_sameRank() throws Exception {
    User user = saveUser();
    Instant yesterday = kstYesterday;

    Review review1 = saveReview(user, saveBook());
    addLikesWithDate(review1, 2, yesterday.plus(1, ChronoUnit.HOURS));
    addCommentsWithDate(review1, 1, yesterday.plus(2, ChronoUnit.HOURS));

    Review review2 = saveReview(user, saveBook());
    addLikesWithDate(review2, 2, yesterday.plus(1, ChronoUnit.HOURS));
    addCommentsWithDate(review2, 1, yesterday.plus(2, ChronoUnit.HOURS));

    Review review3 = saveReview(user, saveBook());
    addLikesWithDate(review3, 1, yesterday.plus(1, ChronoUnit.HOURS));

    JobExecution execution = launchJob(popularReviewDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PopularReview> results = popularReviewRepository.findByPeriodOrderByScoreDesc(Period.DAILY);
    assertThat(results).hasSize(3);
    assertThat(results.stream().filter(r -> r.getRanking() == 1).count()).isEqualTo(2);
    assertThat(results.stream().filter(r -> r.getRanking() == 3).count()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 논리 삭제된 리뷰도 인기 점수 계산에 포함")
  void job_includesDeletedReview() throws Exception {
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    addLikesWithDate(review, 2, kstYesterday.plus(1, ChronoUnit.HOURS));
    addCommentsWithDate(review, 3, kstYesterday.plus(2, ChronoUnit.HOURS));

    transactionTemplate.execute(status -> {
      entityManager.createQuery(
              "UPDATE Review r SET r.deletedAt = :deletedAt WHERE r.id = :id")
          .setParameter("deletedAt", Instant.now())
          .setParameter("id", review.getId())
          .executeUpdate();
      return null;
    });

    JobExecution execution = launchJob(popularReviewDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularReviewRepository.findByPeriod(Period.DAILY)).hasSize(1);
  }

  @Test
  @DisplayName("성공: 기존 데이터 있을 때 활동 0건이면 stale 데이터 삭제")
  void job_staleDataCleared_whenNoActivity() throws Exception {
    Review review = saveReview(saveUser(), saveBook());
    transactionTemplate.execute(status -> {
      PopularReview stale = PopularReview.builder()
          .reviewId(review.getId())
          .period(Period.DAILY)
          .score(BigDecimal.valueOf(1.0))
          .likeCount(1)
          .commentCount(0)
          .calculatedAt(Instant.now())
          .build();
      popularReviewRepository.save(stale);
      return null;
    });

    JobExecution execution = launchJob(popularReviewDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularReviewRepository.findByPeriod(Period.DAILY)).isEmpty();
  }
}