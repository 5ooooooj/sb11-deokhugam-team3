package com.team3.deokhugam.repository.comment;

import static com.team3.deokhugam.domain.comment.CommentTestFactory.comment;
import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.global.config.JpaAuditingConfig;
import java.time.Instant;
import java.util.List;
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

  @Test
  @DisplayName("댓글을 저장하고 ID로 조회할 수 있습니다.")
  void saveAndFindById() {
    Comment comment = comment().content("좋은 리뷰네요").build();

    Comment saved = commentRepository.save(comment);

    assertThat(commentRepository.findById(saved.getId())).isPresent();
  }

  @Test
  @DisplayName("reviewId로 댓글 목록을 조회할 수 있습니다.")
  void findByReviewIdWithCursor_success() {
    Comment comment1 = comment().content("첫 번째 댓글").build();
    Comment comment2 = comment().content("두 번째 댓글").build();
    commentRepository.saveAll(List.of(comment1, comment2));

    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        comment1.getReview().getId(), null, PageRequest.of(0, 10)
    );

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("논리 삭제된 댓글은 조회에서 제외됩니다.")
  void findByReviewIdWithCursor_excludesDeleted() {
    Comment activeComment = comment().content("활성 댓글").build();
    Comment deletedComment = comment()
        .review(activeComment.getReview())
        .content("삭제된 댓글").build();
    commentRepository.saveAll(List.of(activeComment, deletedComment));

    deletedComment.softDelete();
    commentRepository.save(deletedComment);

    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        activeComment.getReview().getId(), null, PageRequest.of(0, 10)
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).isEqualTo("활성 댓글");
  }

  @Test
  @DisplayName("after 파라미터로 커서 페이지네이션이 동작합니다.")
  void findByReviewIdWithCursor_withAfter() {
    Comment comment1 = comment().content("첫 번째 댓글").build();
    commentRepository.save(comment1);

    Instant after = comment1.getCreatedAt().plusNanos(1);

    Comment comment2 = comment()
        .review(comment1.getReview())
        .content("두 번째 댓글").build();
    commentRepository.save(comment2);

    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        comment1.getReview().getId(), after, PageRequest.of(0, 10)
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getContent()).isEqualTo("첫 번째 댓글");
  }

  @Test
  @DisplayName("limit 조건이 적용됩니다.")
  void findByReviewIdWithCursor_withLimit() {
    Comment c1 = comment().content("댓글 1").build();
    Comment c2 = comment().review(c1.getReview()).content("댓글 2").build();
    Comment c3 = comment().review(c1.getReview()).content("댓글 3").build();
    commentRepository.saveAll(List.of(c1, c2, c3));

    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        c1.getReview().getId(), null, PageRequest.of(0, 2)
    );

    assertThat(result).hasSize(2);
  }
}