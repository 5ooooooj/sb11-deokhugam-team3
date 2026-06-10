package com.team3.deokhugam.batch.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
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
import org.springframework.batch.core.JobParameters;
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
public class PopularBookJobTest {

  @Autowired private JobRepository jobRepository;

  @Autowired @Qualifier("popularBookDailyJob") private Job popularBookDailyJob;
  @Autowired @Qualifier("popularBookWeeklyJob") private Job popularBookWeeklyJob;
  @Autowired @Qualifier("popularBookMonthlyJob") private Job popularBookMonthlyJob;
  @Autowired @Qualifier("popularBookAllTimeJob") private Job popularBookAllTimeJob;

  @Autowired private PopularBookRepository popularBookRepository;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private BookRepository bookRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private EntityManager entityManager;
  @Autowired private PlatformTransactionManager transactionManager;

  private TransactionTemplate transactionTemplate;
  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  @BeforeEach
  void setUp() {
    transactionTemplate = new TransactionTemplate(transactionManager);
    new JobRepositoryTestUtils(jobRepository).removeJobExecutions();
    popularBookRepository.deleteAll();
    reviewRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();
  }

  private JobExecution launchJob(Job job) throws Exception {
    TaskExecutorJobLauncher syncLauncher = new TaskExecutorJobLauncher();
    syncLauncher.setJobRepository(jobRepository);
    syncLauncher.setTaskExecutor(new SyncTaskExecutor());
    syncLauncher.afterPropertiesSet();

    JobParameters params = new JobParametersBuilder()
        .addLocalDateTime("runAt", LocalDateTime.now())
        .addLong("nonce", System.nanoTime())
        .toJobParameters();

    JobExecution execution = syncLauncher.run(job, params);
    return execution;
  }

  @AfterEach
  void tearDown() {
    popularBookRepository.deleteAll();
    reviewRepository.deleteAll();
    bookRepository.deleteAll();
    userRepository.deleteAll();
  }

  private Instant kstToday() {
    return LocalDate.now(KST).atStartOfDay(KST).plusHours(1).toInstant();
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

  @Test
  @DisplayName("성공: DAILY Job 실행 후 오늘 리뷰만 집계")
  void daily_job_success() throws Exception {

    User user = saveUser();
    Book book = saveBook();
    saveReviewWithCreatedAt(user, book, 5, "오늘 리뷰", kstToday());

    JobExecution execution = launchJob(popularBookDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularBookRepository.findByPeriod(Period.DAILY)).hasSize(1);
  }

  @Test
  @DisplayName("성공: WEEKLY Job 실행 후 7일 내 리뷰만 집계")
  void weekly_job_success() throws Exception {
    User user1 = saveUser();
    User user2 = saveUser();
    Book book1 = saveBook();
    Book book2 = saveBook();
    saveReviewWithCreatedAt(user1, book1, 5, "3일 전 리뷰", Instant.now().minus(3, ChronoUnit.DAYS));
    saveReviewWithCreatedAt(user2, book2, 4, "8일 전 리뷰", Instant.now().minus(8, ChronoUnit.DAYS));

    JobExecution execution = launchJob(popularBookWeeklyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PopularBook> results = popularBookRepository.findByPeriod(Period.WEEKLY);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getBookId()).isEqualTo(book1.getId());
  }

  @Test
  @DisplayName("성공: MONTHLY Job 실행 후 30일 내 리뷰만 집계")
  void monthly_job_success() throws Exception {
    User user1 = saveUser();
    User user2 = saveUser();
    Book book1 = saveBook();
    Book book2 = saveBook();
    saveReviewWithCreatedAt(user1, book1, 5, "15일 전 리뷰", Instant.now().minus(15, ChronoUnit.DAYS));
    saveReviewWithCreatedAt(user2, book2, 4, "31일 전 리뷰", Instant.now().minus(31, ChronoUnit.DAYS));

    JobExecution execution = launchJob(popularBookMonthlyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PopularBook> results = popularBookRepository.findByPeriod(Period.MONTHLY);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getBookId()).isEqualTo(book1.getId());
  }

  @Test
  @DisplayName("성공: ALL_TIME Job은 기간 무관 전체 리뷰 집계")
  void allTime_job_success() throws Exception {
    User user1 = saveUser();
    User user2 = saveUser();
    Book book1 = saveBook();
    Book book2 = saveBook();
    saveReviewWithCreatedAt(user1, book1, 5, "최근 리뷰", kstToday());
    saveReviewWithCreatedAt(user2, book2, 3, "오래된 리뷰", Instant.now().minus(200, ChronoUnit.DAYS));

    JobExecution execution = launchJob(popularBookAllTimeJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularBookRepository.findByPeriod(Period.ALL_TIME)).hasSize(2);
  }

  @Test
  @DisplayName("성공: 리뷰 없으면 빈 결과로 Job 완료")
  void job_noReview_success() throws Exception {
    JobExecution execution = launchJob(popularBookDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularBookRepository.findByPeriod(Period.DAILY)).isEmpty();
  }

  @Test
  @DisplayName("성공: Job 재실행 시 기존 데이터 삭제 후 새로 저장")
  void job_rerun_success() throws Exception {
    User user = saveUser();
    Book book = saveBook();
    saveReviewWithCreatedAt(user, book, 5, "리뷰", kstToday());

    launchJob(popularBookDailyJob);
    long countAfterFirst = popularBookRepository.findByPeriod(Period.DAILY).size();

    JobExecution execution = launchJob(popularBookDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularBookRepository.findByPeriod(Period.DAILY)).hasSize((int) countAfterFirst);
  }

  @Test
  @DisplayName("성공: 1위 도서의 rank가 1로 저장")
  void job_rankIsOne_forTopBook() throws Exception {
    User user = saveUser();
    Book book = saveBook();
    saveReviewWithCreatedAt(user, book, 5, "리뷰", kstToday());

    JobExecution execution = launchJob(popularBookDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PopularBook> results = popularBookRepository.findByPeriod(Period.DAILY);
    assertThat(results).hasSize(1);
    assertThat(results.get(0).getRanking()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 다수 도서 점수 순 rank 올바르게 저장")
  void job_rankIsCorrect_forMultipleBooks() throws Exception {
    User user1 = saveUser();
    User user2 = saveUser();
    User user3 = saveUser();
    Book book1 = saveBook();
    Book book2 = saveBook();
    Book book3 = saveBook();
    Instant now = kstToday();

    saveReviewWithCreatedAt(user1, book1, 5, "리뷰1", now);
    saveReviewWithCreatedAt(user2, book2, 3, "리뷰2", now);
    saveReviewWithCreatedAt(user3, book3, 1, "리뷰3", now);

    JobExecution execution = launchJob(popularBookDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PopularBook> results = popularBookRepository.findByPeriodOrderByScoreDesc(Period.DAILY);
    assertThat(results).hasSize(3);
    assertThat(results.get(0).getBookId()).isEqualTo(book1.getId());
    assertThat(results.get(0).getRanking()).isEqualTo(1);
    assertThat(results.get(1).getBookId()).isEqualTo(book2.getId());
    assertThat(results.get(1).getRanking()).isEqualTo(2);
    assertThat(results.get(2).getBookId()).isEqualTo(book3.getId());
    assertThat(results.get(2).getRanking()).isEqualTo(3);
  }

  @Test
  @DisplayName("성공: 동점 도서는 같은 rank 부여 (1, 1, 3)")
  void job_sameScore_sameRank() throws Exception {
    User user1 = saveUser();
    User user2 = saveUser();
    User user3 = saveUser();
    Book book1 = saveBook();
    Book book2 = saveBook();
    Book book3 = saveBook();
    Instant now = kstToday();

    saveReviewWithCreatedAt(user1, book1, 5, "리뷰1", now);
    saveReviewWithCreatedAt(user2, book2, 5, "리뷰2", now);
    saveReviewWithCreatedAt(user3, book3, 3, "리뷰3", now);

    JobExecution execution = launchJob(popularBookDailyJob);

    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    List<PopularBook> results = popularBookRepository.findByPeriodOrderByScoreDesc(Period.DAILY);
    assertThat(results).hasSize(3);
    assertThat(results.stream().filter(r -> r.getRanking() == 1).count()).isEqualTo(2);
    assertThat(results.stream().filter(r -> r.getRanking() == 3).count()).isEqualTo(1);
  }
}