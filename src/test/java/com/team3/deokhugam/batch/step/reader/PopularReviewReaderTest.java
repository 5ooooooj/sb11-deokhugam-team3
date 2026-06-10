package com.team3.deokhugam.batch.step.reader;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.dto.PopularReviewRawData;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewLike;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.repository.comment.CommentRepository;
import com.team3.deokhugam.repository.review.ReviewLikeRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
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
class PopularReviewReaderTest {

  @Autowired private PlatformTransactionManager transactionManager;
  @Autowired private EntityManagerFactory entityManagerFactory;
  @Autowired private EntityManager entityManager;
  @Autowired private ReviewRepository reviewRepository;
  @Autowired private ReviewLikeRepository reviewLikeRepository;
  @Autowired private CommentRepository commentRepository;
  @Autowired private BookRepository bookRepository;
  @Autowired private UserRepository userRepository;

  private PopularReviewReader popularReviewReader;
  private TransactionTemplate transactionTemplate;

  @BeforeEach
  void setUp() {
    popularReviewReader = new PopularReviewReader(entityManagerFactory);
    transactionTemplate = new TransactionTemplate(transactionManager);
  }

  @AfterEach
  void tearDown() {
    transactionTemplate.execute(status -> {
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

  @Test
  @DisplayName("성공: 기간 내 좋아요/댓글을 리뷰별로 집계")
  void read_daily_success() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());

    transactionTemplate.execute(status -> {
      User liker1 = userRepository.save(
          new User("liker1-" + UUID.randomUUID() + "@test.com", "좋아요1", "Password1!"));
      User liker2 = userRepository.save(
          new User("liker2-" + UUID.randomUUID() + "@test.com", "좋아요2", "Password1!"));
      User commenter = userRepository.save(
          new User("commenter-" + UUID.randomUUID() + "@test.com", "댓글러", "Password1!"));

      reviewLikeRepository.save(ReviewLike.create(review, liker1));
      reviewLikeRepository.save(ReviewLike.create(review, liker2));
      commentRepository.save(Comment.create(review, commenter, "댓글"));
      return null;
    });

    // when
    JpaPagingItemReader<PopularReviewRawData> reader = popularReviewReader.create(Period.DAILY);
    reader.open(new ExecutionContext());
    PopularReviewRawData result = reader.read();

    // then
    assertThat(result).isNotNull();
    assertThat(result.reviewId()).isEqualTo(review.getId());
    assertThat(result.likeCount()).isEqualTo(2);
    assertThat(result.commentCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 기간 밖 좋아요/댓글은 집계에서 제외")
  void read_excludesOutOfPeriod() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());

    transactionTemplate.execute(status -> {
      User liker = userRepository.save(
          new User("liker-" + UUID.randomUUID() + "@test.com", "좋아요", "Password1!"));
      ReviewLike like = reviewLikeRepository.save(ReviewLike.create(review, liker));

      // 2일 전 좋아요 → DAILY 범위 밖
      entityManager.createQuery(
              "UPDATE ReviewLike rl SET rl.createdAt = :createdAt WHERE rl.id = :id")
          .setParameter("createdAt", Instant.now().minus(2, ChronoUnit.DAYS))
          .setParameter("id", like.getId())
          .executeUpdate();
      return null;
    });

    JpaPagingItemReader<PopularReviewRawData> reader = popularReviewReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    PopularReviewRawData result = reader.read();

    // then
    assertThat(result).isNull(); // 기간 밖이므로 집계 안 됨
  }

  @Test
  @DisplayName("성공: 논리 삭제된 리뷰도 집계에 포함")
  void read_includesDeletedReview() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());

    transactionTemplate.execute(status -> {
      User liker = userRepository.save(
          new User("liker-" + UUID.randomUUID() + "@test.com", "좋아요", "Password1!"));
      reviewLikeRepository.save(ReviewLike.create(review, liker));

      entityManager.createQuery(
              "UPDATE Review r SET r.deletedAt = :deletedAt WHERE r.id = :id")
          .setParameter("deletedAt", Instant.now())
          .setParameter("id", review.getId())
          .executeUpdate();
      return null;
    });

    JpaPagingItemReader<PopularReviewRawData> reader = popularReviewReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    PopularReviewRawData result = reader.read();

    // then
    assertThat(result).isNotNull(); // 논리 삭제 포함
    assertThat(result.likeCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: ALL_TIME은 전체 기간을 집계")
  void read_allTime_success() throws Exception {
    // given
    User user = saveUser();
    Review review = saveReview(user, saveBook());

    transactionTemplate.execute(status -> {
      User liker = userRepository.save(
          new User("liker-" + UUID.randomUUID() + "@test.com", "좋아요", "Password1!"));
      User commenter = userRepository.save(
          new User("commenter-" + UUID.randomUUID() + "@test.com", "댓글러", "Password1!"));

      ReviewLike like = reviewLikeRepository.save(ReviewLike.create(review, liker));
      Comment comment = commentRepository.save(Comment.create(review, commenter, "댓글"));

      // 200일 전 데이터
      entityManager.createQuery(
              "UPDATE ReviewLike rl SET rl.createdAt = :createdAt WHERE rl.id = :id")
          .setParameter("createdAt", Instant.now().minus(200, ChronoUnit.DAYS))
          .setParameter("id", like.getId())
          .executeUpdate();
      entityManager.createQuery(
              "UPDATE Comment c SET c.createdAt = :createdAt WHERE c.id = :id")
          .setParameter("createdAt", Instant.now().minus(200, ChronoUnit.DAYS))
          .setParameter("id", comment.getId())
          .executeUpdate();
      return null;
    });

    JpaPagingItemReader<PopularReviewRawData> reader = popularReviewReader.create(Period.ALL_TIME);
    reader.open(new ExecutionContext());

    // when
    PopularReviewRawData result = reader.read();

    // then
    assertThat(result).isNotNull();
    assertThat(result.likeCount()).isEqualTo(1);
    assertThat(result.commentCount()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 좋아요/댓글이 없으면 결과가 없음")
  void read_noActivity() throws Exception {
    // given - 리뷰만 있고 좋아요/댓글 없음
    User user = saveUser();
    saveReview(user, saveBook());

    JpaPagingItemReader<PopularReviewRawData> reader = popularReviewReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    PopularReviewRawData result = reader.read();

    // then
    assertThat(result).isNull();
  }

  @Test
  @DisplayName("성공: 여러 리뷰를 각각 집계")
  void read_multipleReviews() throws Exception {
    // given
    User user = saveUser();
    Review review1 = saveReview(user, saveBook());
    Review review2 = saveReview(user, saveBook());

    transactionTemplate.execute(status -> {
      User liker1 = userRepository.save(
          new User("liker1-" + UUID.randomUUID() + "@test.com", "좋아요1", "Password1!"));
      User liker2 = userRepository.save(
          new User("liker2-" + UUID.randomUUID() + "@test.com", "좋아요2", "Password1!"));
      User commenter = userRepository.save(
          new User("commenter-" + UUID.randomUUID() + "@test.com", "댓글러", "Password1!"));

      reviewLikeRepository.save(ReviewLike.create(review1, liker1));
      reviewLikeRepository.save(ReviewLike.create(review1, liker2));
      commentRepository.save(Comment.create(review2, commenter, "댓글"));
      return null;
    });

    JpaPagingItemReader<PopularReviewRawData> reader = popularReviewReader.create(Period.DAILY);
    reader.open(new ExecutionContext());

    // when
    List<PopularReviewRawData> results = new ArrayList<>();
    PopularReviewRawData item;
    while ((item = reader.read()) != null) {
      results.add(item);
    }

    // then
    assertThat(results).hasSize(2);

    PopularReviewRawData review1Result = results.stream()
        .filter(r -> r.reviewId().equals(review1.getId()))
        .findFirst().orElseThrow();
    assertThat(review1Result.likeCount()).isEqualTo(2);
    assertThat(review1Result.commentCount()).isEqualTo(0);

    PopularReviewRawData review2Result = results.stream()
        .filter(r -> r.reviewId().equals(review2.getId()))
        .findFirst().orElseThrow();
    assertThat(review2Result.likeCount()).isEqualTo(0);
    assertThat(review2Result.commentCount()).isEqualTo(1);
  }
}