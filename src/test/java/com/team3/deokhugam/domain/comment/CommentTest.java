package com.team3.deokhugam.domain.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.exception.comment.CommentForbiddenException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class CommentTest {

  @Test
  @DisplayName("댓글 엔티티를 생성하면 필수 정보가 정상적으로 저장됩니다.")
  void createComment() {
    // given
    Review review = mock(Review.class);
    User user = mock(User.class);
    String content = "좋은 리뷰네요";

    // when
    Comment comment = Comment.create(review, user, content);

    // then
    assertThat(comment.getReview()).isEqualTo(review);
    assertThat(comment.getUser()).isEqualTo(user);
    assertThat(comment.getContent()).isEqualTo(content);
    assertThat(comment.getDeletedAt()).isNull();
  }

  @Test
  @DisplayName("댓글 내용을 수정하면 변경된 내용이 저장됩니다.")
  void updateComment() {
    // given
    Review review = mock(Review.class);
    User user = mock(User.class);
    Comment comment = Comment.create(review, user, "원본 내용");

    // when
    comment.updateContent("수정된 내용");

    // then
    assertThat(comment.getContent()).isEqualTo("수정된 내용");
  }

  @Test
  @DisplayName("댓글을 논리 삭제하면 deletedAt이 설정됩니다.")
  void softDeleteComment() {
    // given
    Review review = mock(Review.class);
    User user = mock(User.class);
    Comment comment = Comment.create(review, user, "내용");

    // when
    comment.softDelete();

    // then
    assertThat(comment.getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("본인 댓글 소유자 검증에 성공합니다.")
  void validateOwner_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    given(user.getId()).willReturn(userId);
    Review review = mock(Review.class);
    Comment comment = Comment.create(review, user, "내용");

    // when & then
    comment.validateOwner(userId); // 예외 없으면 성공
  }

  @Test
  @DisplayName("다른 사용자가 댓글 소유자 검증 시 예외가 발생합니다.")
  void validateOwner_forbidden() {
    // given
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    User user = mock(User.class);
    given(user.getId()).willReturn(userId);
    Review review = mock(Review.class);
    Comment comment = Comment.create(review, user, "내용");

    // when & then
    assertThatThrownBy(() -> comment.validateOwner(otherUserId))
        .isInstanceOf(CommentForbiddenException.class);
  }

  @Test
  @DisplayName("탈퇴한 사용자(user=null) 댓글 소유자 검증 시 예외가 발생합니다.")
  void validateOwner_nullUser() {
    // given
    Comment comment = Comment.create(mock(Review.class), null, "내용");

    // when & then
    assertThatThrownBy(() -> comment.validateOwner(UUID.randomUUID()))
        .isInstanceOf(CommentForbiddenException.class);
  }
}