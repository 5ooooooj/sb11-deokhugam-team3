package com.team3.deokhugam.batch.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;
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
public class PopularBookJobTest {

  @Autowired
  private JobLauncherTestUtils jobLauncherTestUtils;

  @Autowired
  private JobRepositoryTestUtils jobRepositoryTestUtils;

  @Autowired
  private Job popularBookJob;

  @Autowired
  private PopularBookRepository popularBookRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private PlatformTransactionManager transactionManager;

  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    jobLauncherTestUtils.setJob(popularBookJob);
    transactionTemplate = new TransactionTemplate(transactionManager);
    jobRepositoryTestUtils.removeJobExecutions();
    transactionTemplate.execute(status -> {
      popularBookRepository.deleteAll();
      reviewRepository.deleteAll();
      return null;
    });
  }

  @Test
  @DisplayName("성공: Job 실행 후 POPULAR_BOOKS에 기간별 결과가 저장")
  void job_success() throws Exception {
    // given
    UUID bookId1 = UUID.randomUUID();
    UUID bookId2 = UUID.randomUUID();

    transactionTemplate.execute(status -> {
      // DAILY 범위 (1시간 전) - 기본 createdAt 그대로 사용
      reviewRepository.save(Review.create(UUID.randomUUID(), bookId1, 5, "일간 리뷰"));

      // WEEKLY 범위 (3일 전)
      Review weeklyReview = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId1, 3, "주간 리뷰"));
      entityManager.createQuery(
              "UPDATE Review r SET r.createdAt = :createdAt WHERE r.id = :id")
          .setParameter("createdAt", Instant.now().minus(3, ChronoUnit.DAYS))
          .setParameter("id", weeklyReview.getId())
          .executeUpdate();

      // MONTHLY 범위 (15일 전)
      Review monthlyReview = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId2, 4, "월간 리뷰"));
      entityManager.createQuery(
              "UPDATE Review r SET r.createdAt = :createdAt WHERE r.id = :id")
          .setParameter("createdAt", Instant.now().minus(15, ChronoUnit.DAYS))
          .setParameter("id", monthlyReview.getId())
          .executeUpdate();

      // ALL_TIME 범위 (200일 전)
      Review allTimeReview = reviewRepository.save(
          Review.create(UUID.randomUUID(), bookId2, 2, "역대 리뷰"));
      entityManager.createQuery(
              "UPDATE Review r SET r.createdAt = :createdAt WHERE r.id = :id")
          .setParameter("createdAt", Instant.now().minus(200, ChronoUnit.DAYS))
          .setParameter("id", allTimeReview.getId())
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

    assertThat(popularBookRepository.findByPeriod(Period.DAILY)).hasSize(1);
    assertThat(popularBookRepository.findByPeriod(Period.WEEKLY)).hasSize(1);
    assertThat(popularBookRepository.findByPeriod(Period.MONTHLY)).hasSize(2);
    assertThat(popularBookRepository.findByPeriod(Period.ALL_TIME)).hasSize(2);
  }

  @Test
  @DisplayName("성공: Job 재실행 시 기존 데이터를 삭제하고 새로 저장")
  void job_rerun_success() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    transactionTemplate.execute(status -> {
      reviewRepository.save(Review.create(UUID.randomUUID(), bookId, 5, "리뷰"));
      return null;
    });

    JobParameters params1 = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now().minusDays(1))
        .toJobParameters();
    jobLauncherTestUtils.launchJob(params1);

    long countAfterFirst = popularBookRepository.count();

    // when
    JobParameters params2 = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now())
        .toJobParameters();
    JobExecution execution = jobLauncherTestUtils.launchJob(params2);

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularBookRepository.count()).isEqualTo(countAfterFirst);
  }

  private Review createReview(UUID bookId, int rating) {
    return Review.create(UUID.randomUUID(), bookId, rating, "테스트리뷰");
  }

  @Test
  @DisplayName("성공: 리뷰가 없으면 빈 결과로 Job이 완료")
  void job_noReview_success() throws Exception {
    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularBookRepository.count()).isZero();
  }

  @Test
  @DisplayName("성공: 1위 도서의 rank가 1로 저장")
  void job_rankIsOne_forTopBook() throws Exception {
    // given
    UUID bookId = UUID.randomUUID();

    transactionTemplate.execute(status -> {
      reviewRepository.save(Review.create(UUID.randomUUID(), bookId, 5, "리뷰"));
      return null;
    });

    // 저장 확인
    System.out.println("저장된 리뷰 수: " + reviewRepository.count());

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );

    System.out.println("Job 상태: " + execution.getStatus());
    System.out.println("DAILY 결과 수: " + popularBookRepository.findByPeriod(Period.DAILY).size());
    System.out.println("ALL_TIME 결과 수: " + popularBookRepository.findByPeriod(Period.ALL_TIME).size());


    // then
    List<PopularBook> dailyResults = popularBookRepository.findByPeriod(Period.DAILY);
    assertThat(dailyResults).hasSize(1);
    assertThat(dailyResults.get(0).getRank()).isEqualTo(1);
  }
}
