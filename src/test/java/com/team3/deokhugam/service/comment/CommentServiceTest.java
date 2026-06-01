package com.team3.deokhugam.service.comment;

import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.comment.CommentCreateRequest;
import com.team3.deokhugam.dto.comment.CommentDto;
import com.team3.deokhugam.dto.comment.CommentUpdateRequest;
import com.team3.deokhugam.exception.comment.CommentForbiddenException;
import com.team3.deokhugam.exception.comment.CommentNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.comment.CommentRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import com.team3.deokhugam.service.notification.NotificationService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

  @Mock
  private CommentRepository commentRepository;
  @Mock
  private ReviewRepository reviewRepository;
  @Mock
  private UserRepository userRepository;
  @Mock
  private NotificationService notificationService;
  @InjectMocks
  private CommentServiceImpl commentService;

  // ───────────────────────────────────────────
  // create()
  // ───────────────────────────────────────────

  @Test
  @DisplayName("댓글 등록 성공")
  void create_success() {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentCreateRequest request = new CommentCreateRequest(reviewId, userId, "좋은 리뷰네요");

    Review review = mock(Review.class);
    User user = mock(User.class);
    given(user.getId()).willReturn(userId);
    given(user.getNickname()).willReturn("테스트유저");
    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(commentRepository.save(any())).willReturn(Comment.create(review, user, "좋은 리뷰네요"));

    // when
    CommentDto result = commentService.create(request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.content()).isEqualTo("좋은 리뷰네요");
    verify(notificationService).createCommentNotification(reviewId, userId);
  }

  // ───────────────────────────────────────────
  // update()
  // ───────────────────────────────────────────

  @Test
  @DisplayName("본인 댓글 수정 성공")
  void update_success() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");

    User user = mock(User.class);
    given(user.getId()).willReturn(userId);
    given(user.getNickname()).willReturn("테스트유저");
    Review review = mock(Review.class);
    given(review.getId()).willReturn(UUID.randomUUID());

    Comment comment = Comment.create(review, user, "원본 내용");
    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when
    CommentDto result = commentService.update(commentId, userId, request);

    // then
    assertThat(result).isNotNull();
    assertThat(result.content()).isEqualTo("수정된 내용");
  }

  @Test
  @DisplayName("타인 댓글 수정 시 예외")
  void update_forbidden() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");

    User owner = mock(User.class);
    given(owner.getId()).willReturn(ownerId);
    Review review = mock(Review.class);

    Comment comment = Comment.create(review, owner, "원본");
    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> commentService.update(commentId, otherId, request))
        .isInstanceOf(CommentForbiddenException.class);
  }

  @Test
  @DisplayName("논리 삭제된 댓글 수정 시 예외")
  void update_alreadyDeleted() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");

    // user.getId() stubbing 제거!
    User user = mock(User.class);
    Review review = mock(Review.class);

    Comment comment = Comment.create(review, user, "원본");
    comment.softDelete();
    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> commentService.update(commentId, userId, request))
        .isInstanceOf(CommentNotFoundException.class);
  }

  // ───────────────────────────────────────────
  // delete()
  // ───────────────────────────────────────────

  @Test
  @DisplayName("댓글 논리 삭제 성공")
  void delete_success() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User user = mock(User.class);
    given(user.getId()).willReturn(userId);
    Review review = mock(Review.class);

    Comment comment = Comment.create(review, user, "내용");
    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when
    commentService.delete(commentId, userId);

    // then
    assertThat(comment.isDeleted()).isTrue();
    assertThat(comment.getDeletedAt()).isNotNull();
  }

  @Test
  @DisplayName("타인 댓글 삭제 시 예외")
  void delete_forbidden() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();

    User owner = mock(User.class);
    given(owner.getId()).willReturn(ownerId);
    Review review = mock(Review.class);

    Comment comment = Comment.create(review, owner, "내용");
    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when & then
    assertThatThrownBy(() -> commentService.delete(commentId, otherId))
        .isInstanceOf(CommentForbiddenException.class);
  }

  // ───────────────────────────────────────────
  // findAll()
  // ───────────────────────────────────────────

  @Test
  @DisplayName("댓글 목록 조회 - 첫 페이지 (hasNext=false)")
  void findAll_firstPage() {
    // given
    UUID reviewId = UUID.randomUUID();
    Review review = mock(Review.class);
    given(review.getId()).willReturn(reviewId);

    User user1 = mock(User.class);
    User user2 = mock(User.class);

    List<Comment> comments = List.of(
        Comment.create(review, user1, "내용1"),
        Comment.create(review, user2, "내용2")
    );
    given(commentRepository.findByReviewIdWithCursor(
        eq(reviewId), isNull(), any())
    ).willReturn(comments);

    // when
    CursorPageResponse<CommentDto> result =
        commentService.findAll(reviewId, null, 10);

    // then
    assertThat(result.hasNext()).isFalse();
    assertThat(result.content()).hasSize(2);
  }

  @Test
  @DisplayName("댓글 목록 조회 - 다음 페이지 존재 (hasNext=true)")
  void findAll_hasNext() {
    // given
    UUID reviewId = UUID.randomUUID();
    int size = 2;
    Review review = mock(Review.class);
    given(review.getId()).willReturn(reviewId);

    User user1 = mock(User.class);
    User user2 = mock(User.class);
    User user3 = mock(User.class);

    List<Comment> comments = List.of(
        Comment.create(review, user1, "내용1"),
        Comment.create(review, user2, "내용2"),
        Comment.create(review, user3, "내용3")
    );
    given(commentRepository.findByReviewIdWithCursor(
        eq(reviewId), isNull(), any())
    ).willReturn(comments);

    // when
    CursorPageResponse<CommentDto> result =
        commentService.findAll(reviewId, null, size);

    // then
    assertThat(result.hasNext()).isTrue();
    assertThat(result.content()).hasSize(size);
  }

  // hardDelete 테스트 없었는데 추가
  @Test
  @DisplayName("댓글 물리 삭제 성공")
  void hardDelete_success() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User user = mock(User.class);
    given(user.getId()).willReturn(userId);
    Review review = mock(Review.class);

    Comment comment = Comment.create(review, user, "내용");
    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when
    commentService.hardDelete(commentId, userId);

    // then
    verify(commentRepository).deleteById(commentId);
  }
  @Test
  @DisplayName("탈퇴한 사용자 댓글 조회 시 닉네임 탈퇴한 사용자로 표시")
  void findById_deletedUser() {
    // given
    UUID commentId = UUID.randomUUID();

    Review review = mock(Review.class);
    given(review.getId()).willReturn(UUID.randomUUID());

    Comment comment = Comment.create(review, null, "내용");
    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when
    CommentDto result = commentService.findById(commentId);

    // then
    assertThat(result.userNickname()).isEqualTo("탈퇴한 사용자");
    assertThat(result.userId()).isNull();
  }
}