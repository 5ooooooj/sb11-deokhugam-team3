package com.team3.deokhugam.repository.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewTestFactory;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.global.config.JpaAuditingConfig;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class CommentRepositoryTest {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private jakarta.persistence.EntityManager entityManager;

  private Book persistBook() {
    Book book = new Book(
        UUID.randomUUID(), "테스트 도서", "테스트 저자", "테스트 설명",
        "테스트 출판사", LocalDate.of(2026, 1, 1), null, null);
    entityManager.persist(book);
    return book;
  }

  @Test
  @DisplayName("댓글을 저장하고 ID로 조회할 수 있습니다.")
  void saveAndFindById() {
    User user = userRepository.save(new User("test1@test.com", "테스터1", "Password1!"));
    Book book = persistBook();
    Review review = reviewRepository.save(
        ReviewTestFactory.review().user(user).book(book).build());
    Comment comment = Comment.create(review, user, "좋은 리뷰네요");

    Comment saved = commentRepository.save(comment);

    assertThat(commentRepository.findById(saved.getId())).isPresent();
  }

  @Test
  @DisplayName("reviewId로 댓글 목록을 조회할 수 있습니다.")
  void findByReviewIdWithCursor_success() {
    User user = userRepository.save(new User("test2@test.com", "테스터2", "Password1!"));
    Book book = persistBook();
    Review review = reviewRepository.save(
        ReviewTestFactory.review().user(user).book(book).build());

    commentRepository.saveAll(List.of(
        Comment.create(review, user, "첫 번째 댓글"),
        Comment.create(review, user, "두 번째 댓글")
    ));

    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        review.getId(), null, PageRequest.of(0, 10)
    );

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("논리 삭제된 댓글은 조회에서 제외됩니다.")
  void findByReviewIdWithCursor_excludesDeleted() {
    User user = userRepository.save(new User("test3@test.com", "테스터3", "Password1!"));
    Book book = persistBook();
    Review review = reviewRepository.save(
        ReviewTestFactory.review().user(user).book(book).build());

    Comment activeComment = commentRepository.save(Comment.create(review, user, "활성 댓글"));
    Comment deletedComment = commentRepository.save(Comment.create(review, user, "삭제된 댓글"));

    deletedComment.softDelete();
    commentRepository.save(deletedComment);

    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        review.getId(), null, PageRequest.of(0, 10)
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).isEqualTo("활성 댓글");
  }

  @Test
  @DisplayName("after 파라미터로 커서 페이지네이션이 동작합니다.")
  void findByReviewIdWithCursor_withAfter() {
    User user = userRepository.save(new User("test4@test.com", "테스터4", "Password1!"));
    Book book = persistBook();
    Review review = reviewRepository.save(
        ReviewTestFactory.review().user(user).book(book).build());

    Comment comment1 = commentRepository.save(Comment.create(review, user, "첫 번째 댓글"));
    Comment comment2 = commentRepository.save(Comment.create(review, user, "두 번째 댓글"));

    // createdAt 직접 업데이트
    entityManager.createQuery(
            "UPDATE Comment c SET c.createdAt = :createdAt WHERE c.id = :id")
        .setParameter("createdAt", Instant.parse("2024-01-01T00:00:00Z"))
        .setParameter("id", comment1.getId())
        .executeUpdate();

    entityManager.createQuery(
            "UPDATE Comment c SET c.createdAt = :createdAt WHERE c.id = :id")
        .setParameter("createdAt", Instant.parse("2024-01-02T00:00:00Z"))
        .setParameter("id", comment2.getId())
        .executeUpdate();

    entityManager.flush();
    entityManager.clear();

    Instant after = Instant.parse("2024-01-01T12:00:00Z");

    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        review.getId(), after, PageRequest.of(0, 10)
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).isEqualTo("첫 번째 댓글");
  }

  @Test
  @DisplayName("limit 조건이 적용됩니다.")
  void findByReviewIdWithCursor_withLimit() {
    User user = userRepository.save(new User("test5@test.com", "테스터5", "Password1!"));
    Book book = persistBook();
    Review review = reviewRepository.save(
        ReviewTestFactory.review().user(user).book(book).build());

    commentRepository.saveAll(List.of(
        Comment.create(review, user, "댓글 1"),
        Comment.create(review, user, "댓글 2"),
        Comment.create(review, user, "댓글 3")
    ));

    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        review.getId(), null, PageRequest.of(0, 2)
    );

    assertThat(result).hasSize(2);
  }
}