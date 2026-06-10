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
import java.time.Instant;
import java.time.LocalDate;
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
public class PopularReviewJobTest {

  @Autowired private JobLauncherTestUtils jobLauncherTestUtils;
  @Autowired private JobRepositoryTestUtils jobRepositoryTestUtils;
  @Autowired private Job popularReviewJob;
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
    jobLauncherTestUtils.setJob(popularReviewJob);
    transactionTemplate = new TransactionTemplate(transactionManager);
    jobRepositoryTestUtils.removeJobExecutions();
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

  // 리뷰 createdAt을 과거로 밀어서 기간 범위 조정
  private void shiftReviewCreatedAt(UUID reviewId, long daysAgo) {
    transactionTemplate.execute(status -> {
      entityManager.createQuery(
              "UPDATE Review r SET r.createdAt = :createdAt WHERE r.id = :id")
          .setParameter("createdAt", Instant.now().minus(daysAgo, ChronoUnit.DAYS))
          .setParameter("id", reviewId)
          .executeUpdate();
      return null;
    });
  }

  private void addLikes(Review review, int count) {
    transactionTemplate.execute(status -> {
      for (int i = 0; i < count; i++) {
        User liker = userRepository.save(
            new User("liker-" + UUID.randomUUID() + "@test.com", "좋아요유저", "Password1!"));
        reviewLikeRepository.save(ReviewLike.create(review, liker));
      }
      return null;
    });
  }

  private void addComments(Review review, int count) {
    transactionTemplate.execute(status -> {
      for (int i = 0; i < count; i++) {
        User commenter = userRepository.save(
            new User("commenter-" + UUID.randomUUID() + "@test.com", "댓글유저", "Password1!"));
        commentRepository.save(Comment.create(review, commenter, "댓글" + i));
      }
      return null;
    });
  }

  private void addLikesWithDate(Review review, int count, long daysAgo) {
    transactionTemplate.execute(status -> {
      for (int i = 0; i < count; i++) {
        User liker = userRepository.save(
            new User("liker-" + UUID.randomUUID() + "@test.com", "좋아요유저", "Password1!"));
        ReviewLike like = reviewLikeRepository.save(ReviewLike.create(review, liker));
        entityManager.createQuery(
                "UPDATE ReviewLike rl SET rl.createdAt = :createdAt WHERE rl.id = :id")
            .setParameter("createdAt", Instant.now().minus(daysAgo, ChronoUnit.DAYS))
            .setParameter("id", like.getId())
            .executeUpdate();
      }
      return null;
    });
  }

  private void addCommentsWithDate(Review review, int count, long daysAgo) {
    transactionTemplate.execute(status -> {
      for (int i = 0; i < count; i++) {
        User commenter = userRepository.save(
            new User("commenter-" + UUID.randomUUID() + "@test.com", "댓글유저", "Password1!"));
        Comment comment = commentRepository.save(Comment.create(review, commenter, "댓글" + i));
        entityManager.createQuery(
                "UPDATE Comment c SET c.createdAt = :createdAt WHERE c.id = :id")
            .setParameter("createdAt", Instant.now().minus(daysAgo, ChronoUnit.DAYS))
            .setParameter("id", comment.getId())
            .executeUpdate();
      }
      return null;
    });
  }

  @Test
  @DisplayName("성공: Job 실행 후 POPULAR_REVIEWS에 기간별 결과가 저장")
  void job_success() throws Exception {
    // given
    User user = saveUser();

    // DAILY - 오늘 좋아요/댓글 발생 -> DAILY에만 잡힘
    Review dailyReview = saveReview(user, saveBook());
    addLikes(dailyReview, 2);      // 오늘 생성
    addComments(dailyReview, 1);   // 오늘 생성

    // WEEKLY - 3일 전 좋아요 발생 -> WEEKLY/MONTHLY/ALL_TIME에 잡힘
    Review weeklyReview = saveReview(user, saveBook());
    addLikesWithDate(weeklyReview, 1, 3);   // 3일 전

    // MONTHLY - 15일 전 댓글 발생 -> MONTHLY/ALL_TIME에 잡힘
    Review monthlyReview = saveReview(user, saveBook());
    addCommentsWithDate(monthlyReview, 2, 15);  // 15일 전

    // ALL_TIME - 200일 전 좋아요 발생 -> ALL_TIME에만 잡힘
    Review allTimeReview = saveReview(user, saveBook());
    addLikesWithDate(allTimeReview, 3, 200);  // 200일 전

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularReviewRepository.findByPeriod(Period.DAILY)).hasSize(1);
    assertThat(popularReviewRepository.findByPeriod(Period.WEEKLY)).hasSize(2);
    assertThat(popularReviewRepository.findByPeriod(Period.MONTHLY)).hasSize(3);
    assertThat(popularReviewRepository.findByPeriod(Period.ALL_TIME)).hasSize(4);
  }

  @Test
  @DisplayName("성공: Job 재실행 시 기존 데이터를 삭제하고 새로 저장")
  void job_rerun_success() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    addLikes(review, 1);

    JobParameters params1 = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now().minusDays(1))
        .toJobParameters();
    jobLauncherTestUtils.launchJob(params1);

    long countAfterFirst = popularReviewRepository.count();

    // when
    JobParameters params2 = new JobParametersBuilder()
        .addLocalDate("targetDate", LocalDate.now())
        .toJobParameters();
    JobExecution execution = jobLauncherTestUtils.launchJob(params2);

    // then
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    assertThat(popularReviewRepository.count()).isEqualTo(countAfterFirst);
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
    assertThat(popularReviewRepository.count()).isZero();
  }

  @Test
  @DisplayName("성공: 1위 리뷰의 rank가 1로 저장")
  void job_rankIsOne_forTopReview() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    addLikes(review, 3);
    addComments(review, 2);

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // then
    List<PopularReview> dailyResults = popularReviewRepository.findByPeriod(Period.DAILY);
    assertThat(dailyResults).hasSize(1);
    assertThat(dailyResults.get(0).getRanking()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 다수 리뷰 있을 때 점수 기반 순위가 올바르게 저장됨")
  void job_globalRankIsCorrect_forMultipleReviews() throws Exception {
    // given
    User user = saveUser();

    Review review1 = saveReview(user, saveBook());
    addLikes(review1, 3);
    addComments(review1, 5);

    Review review2 = saveReview(user, saveBook());
    addLikes(review2, 5);
    addComments(review2, 2);

    Review review3 = saveReview(user, saveBook());
    addLikes(review3, 1);

    // when
    JobExecution execution = jobLauncherTestUtils.launchJob(
        new JobParametersBuilder()
            .addLocalDate("targetDate", LocalDate.now())
            .toJobParameters()
    );
    assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

    // then
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
  @DisplayName("성공: 논리 삭제된 리뷰도 인기 점수 계산에 포함")
  void job_includesDeletedReview() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());
    addLikes(review, 2);
    addComments(review, 3);

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
    // 인기 점수 계산 시 논리 삭제 데이터 포함
    assertThat(popularReviewRepository.findByPeriod(Period.DAILY)).hasSize(1);
  }
}