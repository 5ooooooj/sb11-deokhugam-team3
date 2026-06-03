package com.team3.deokhugam.repository.comment;

import static com.team3.deokhugam.domain.comment.CommentTestFactory.comment;
import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewTestFactory;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.repository.BaseRepositoryTest;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class CommentRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private CommentRepository commentRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Test
  @DisplayName("댓글을 저장하고 ID로 조회할 수 있습니다.")
  void saveAndFindById() {
    User user = userRepository.save(new User("test1@test.com", "테스터1", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    Comment saved = commentRepository.save(
        comment().user(user).review(review).content("좋은 리뷰네요").build()
    );

    assertThat(commentRepository.findById(saved.getId())).isPresent();
  }

  @Test
  @DisplayName("reviewId로 댓글 목록을 조회할 수 있습니다.")
  void findByReviewIdWithCursor_success() {
    User user = userRepository.save(new User("test2@test.com", "테스터2", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    commentRepository.saveAll(List.of(
        comment().user(user).review(review).content("첫 번째 댓글").build(),
        comment().user(user).review(review).content("두 번째 댓글").build()
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
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    Comment activeComment = commentRepository.save(
        comment().user(user).review(review).content("활성 댓글").build()
    );
    Comment deletedComment = commentRepository.save(
        comment().user(user).review(review).content("삭제된 댓글").build()
    );

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
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    Comment comment1 = commentRepository.save(
        comment().user(user).review(review).content("첫 번째 댓글").build()
    );

    Instant after = comment1.getCreatedAt().plusNanos(1);

    commentRepository.save(
        comment().user(user).review(review).content("두 번째 댓글").build()
    );

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
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    commentRepository.saveAll(List.of(
        comment().user(user).review(review).content("댓글 1").build(),
        comment().user(user).review(review).content("댓글 2").build(),
        comment().user(user).review(review).content("댓글 3").build()
    ));

    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        review.getId(), null, PageRequest.of(0, 2)
    );

    assertThat(result).hasSize(2);
  }
}