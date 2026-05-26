package com.team3.deokhugam.service.comment;

import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.dto.comment.CommentCreateRequest;
import com.team3.deokhugam.dto.comment.CommentDto;
import com.team3.deokhugam.dto.comment.CommentUpdateRequest;
import com.team3.deokhugam.exception.comment.CommentForbiddenException;
import com.team3.deokhugam.exception.comment.CommentNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.comment.CommentRepository;
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

@ExtendWith(MockitoExtension.class)
public class CommentServiceTest {

  @Mock
  private CommentRepository commentRepository;
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

    Comment comment = Comment.create(reviewId, userId, "좋은 리뷰네요");
    given(commentRepository.save(any())).willReturn(comment);

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

    Comment comment = Comment.create(UUID.randomUUID(), userId, "원본 내용");
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

    Comment comment = Comment.create(UUID.randomUUID(), ownerId, "원본");
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

    Comment comment = Comment.create(UUID.randomUUID(), userId, "원본");
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

    Comment comment = Comment.create(UUID.randomUUID(), userId, "내용");
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

    Comment comment = Comment.create(UUID.randomUUID(), ownerId, "내용");
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

    List<Comment> comments = List.of(
        Comment.create(reviewId, UUID.randomUUID(), "내용1"),
        Comment.create(reviewId, UUID.randomUUID(), "내용2")
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
        Comment.create(reviewId, UUID.randomUUID(), "내용1"),
        Comment.create(reviewId, UUID.randomUUID(), "내용2"),
        Comment.create(reviewId, UUID.randomUUID(), "내용3")
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