package com.team3.deokhugam.repository.comment;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.global.config.JpaAuditingConfig;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
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
  private TestEntityManager entityManager;

  private Review review;
  private User user;

  @BeforeEach
  void setUp() {
    user = new User("test@test.com", "테스터", "Password1!");
    entityManager.persist(user);

    review = Review.create(user.getId(), java.util.UUID.randomUUID(), 5, "좋은 책이에요");
    entityManager.persist(review);

    entityManager.flush();
  }

  @Test
  @DisplayName("댓글을 저장하고 ID로 조회할 수 있습니다.")
  void saveAndFindById() {
    // given
    Comment comment = Comment.create(review, user, "좋은 리뷰네요");

    // when
    Comment saved = commentRepository.save(comment);

    // then
    assertThat(commentRepository.findById(saved.getId())).isPresent();
  }

  @Test
  @DisplayName("reviewId로 댓글 목록을 조회할 수 있습니다.")
  void findByReviewIdWithCursor_success() {
    // given
    Comment comment1 = Comment.create(review, user, "첫 번째 댓글");
    Comment comment2 = Comment.create(review, user, "두 번째 댓글");
    commentRepository.saveAll(List.of(comment1, comment2));
    entityManager.flush();

    // when
    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        review.getId(), null, PageRequest.of(0, 10)
    );

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("논리 삭제된 댓글은 조회에서 제외됩니다.")
  void findByReviewIdWithCursor_excludesDeleted() {
    // given
    Comment activeComment = Comment.create(review, user, "활성 댓글");
    Comment deletedComment = Comment.create(review, user, "삭제된 댓글");
    commentRepository.saveAll(List.of(activeComment, deletedComment));

    deletedComment.softDelete();
    commentRepository.save(deletedComment);
    entityManager.flush();

    // when
    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        review.getId(), null, PageRequest.of(0, 10)
    );

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).isEqualTo("활성 댓글");
  }

  @Test
  @DisplayName("after 파라미터로 커서 페이지네이션이 동작합니다.")
  void findByReviewIdWithCursor_withAfter() {
    // given
    Comment comment1 = Comment.create(review, user, "첫 번째 댓글");
    commentRepository.save(comment1);
    entityManager.flush();

    Instant after = Instant.now();

    Comment comment2 = Comment.create(review, user, "두 번째 댓글");
    commentRepository.save(comment2);
    entityManager.flush();

    // when
    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        review.getId(), after, PageRequest.of(0, 10)
    );

    // then
    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).isEqualTo("첫 번째 댓글");
  }

  @Test
  @DisplayName("limit 조건이 적용됩니다.")
  void findByReviewIdWithCursor_withLimit() {
    // given
    commentRepository.saveAll(List.of(
        Comment.create(review, user, "댓글 1"),
        Comment.create(review, user, "댓글 2"),
        Comment.create(review, user, "댓글 3")
    ));
    entityManager.flush();

    // when
    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        review.getId(), null, PageRequest.of(0, 2)
    );

    // then
    assertThat(result).hasSize(2);
  }
}