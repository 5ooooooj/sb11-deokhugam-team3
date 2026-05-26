package com.team3.deokhugam.service.comment;

import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.comment.CommentCreateRequest;
import com.team3.deokhugam.dto.comment.CommentUpdateRequest;
import com.team3.deokhugam.dto.comment.CommentDto;
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

  // ------------------------------------------
  // create()
  //-------------------------------------------

  @Test
  @DisplayName("댓글 등록 성공")
  void create_success() {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentCreateRequest request = new CommentCreateRequest(reviewId, userId, "좋은 리뷰네요");

    Review review = mock(Review.class);
    User user = mock(User.class);
    Comment comment = mock(Comment.class);

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(userRepository.findById(userId)).willReturn(Optional.of(user));
    given(commentRepository.save(any())).willReturn(comment);
    given(comment.getContent()).willReturn("좋은 리뷰네요");

    //when
    CommentDto result = commentService.create(request);

    //then
    assertThat(result).isNotNull();
    assertThat(result.content()).isEqualTo("좋은 리뷰네요");
    verify(notificationService).createCommentNotification(reviewId, userId);
  }

  // TODO: Exception class 올라오면 추가예
  // void create_reviewNotFound()
  // void create_userNotFound()

  //------------------------------------------
  // update()
  //------------------------------------------

  @Test
  @DisplayName("본인 댓글 수정 성공")
  void update_success() {
    // given
    UUID commentId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");

    User user = mock(User.class);
    given(user.getId()).willReturn(userId);

    Comment comment = mock(Comment.class);
    given(comment.getUser()).willReturn(user);
    given(comment.isDeleted()).willReturn(false);
    given(comment.getContent()).willReturn("수정된 내용");
    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when
    CommentDto result = commentService.update(commentId, userId, request);

    // then
    assertThat(result).isNotNull();
    verify(comment).updateContent("수정된 내용");
  }
  // TODO: Exception class 올라오면 추가
  // void update_forbidden()
  // void update_alreadyDeleted()

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

    Comment comment = mock(Comment.class);
    given(comment.getUser()).willReturn(user);
    given(comment.isDeleted()).willReturn(false);
    given(commentRepository.findById(commentId)).willReturn(Optional.of(comment));

    // when
    commentService.delete(commentId, userId);

    // then
    verify(comment).softDelete();
  }

  // TODO: Exception 클래스 올라오면 추가
  // void delete_forbidden()

  // ───────────────────────────────────────────
  // findAll()
  // ───────────────────────────────────────────

  @Test
  @DisplayName("댓글 목록 조회 - 첫 페이지 (hasNext=false)")
  void findAll_firstPage() {
    // given
    UUID reviewId = UUID.randomUUID();

    List<Comment> comments = List.of(
        mock(Comment.class),
        mock(Comment.class)
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

    List<Comment> comments = List.of(
        mock(Comment.class),
        mock(Comment.class),
        mock(Comment.class)
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


}
