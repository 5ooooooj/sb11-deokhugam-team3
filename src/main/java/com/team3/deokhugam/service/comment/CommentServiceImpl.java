package com.team3.deokhugam.service.comment;

import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.dto.comment.CommentCreateRequest;
import com.team3.deokhugam.dto.comment.CommentDto;
import com.team3.deokhugam.dto.comment.CommentUpdateRequest;
import com.team3.deokhugam.exception.comment.CommentNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.comment.CommentRepository;
import com.team3.deokhugam.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentServiceImpl implements CommentService {

  private final CommentRepository commentRepository;
  private final NotificationService notificationService;

  @Override
  public CommentDto create(CommentCreateRequest request) {
    // 리뷰 존재 여부는 나중에 ReviewRepository 연동 후 추가
    Comment comment = Comment.create(
        request.reviewId(),
        request.userId(),
        request.content()
    );
    commentRepository.save(comment);

    // 알림 트리거
    notificationService.createCommentNotification(
        request.reviewId(), request.userId()
    );

    return toDto(comment);
  }

  @Override
  public CommentDto update(UUID commentId, UUID requestUserId,
      CommentUpdateRequest request) {
    Comment comment = findComment(commentId);

    // 삭제 여부 확인
    if (comment.isDeleted()) {
      throw new CommentNotFoundException();
    }

    // 본인 확인
    comment.validateOwner(requestUserId);

    comment.updateContent(request.content());
    return toDto(comment);
  }

  @Override
  public void delete(UUID commentId, UUID requestUserId) {
    Comment comment = findComment(commentId);

    // 삭제 여부 확인
    if (comment.isDeleted()) {
      throw new CommentNotFoundException();
    }

    // 본인 확인
    comment.validateOwner(requestUserId);

    comment.softDelete();
  }

  @Override
  public void hardDelete(UUID commentId) {
    commentRepository.deleteById(commentId);
  }

  @Override
  @Transactional(readOnly = true)
  public CommentDto findById(UUID commentId) {
    Comment comment = findComment(commentId);
    if (comment.isDeleted()) {
      throw new CommentNotFoundException();
    }
    return toDto(comment);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<CommentDto> findAll(UUID reviewId,
      Instant after, int size) {
    if (size < 1) {
      throw new IllegalArgumentException("size는 1 이상이어야 합니다.");
    }
    List<Comment> result = commentRepository.findByReviewIdWithCursor(
        reviewId, after, PageRequest.of(0, size + 1)
    );

    boolean hasNext = result.size() > size;
    List<Comment> content = hasNext
        ? result.subList(0, size)
        : result;

    String nextCursor = hasNext
        ? content.get(content.size() - 1).getId() != null
          ? content.get(content.size() - 1).getId().toString()
        : null
        : null;

    Instant nextAfter = hasNext
        ? content.get(content.size() - 1).getCreatedAt()
        : null;

    return new CursorPageResponse<>(
        content.stream().map(this::toDto).toList(),
        nextCursor,
        nextAfter,
        size,
        (long) content.size(),
        hasNext
    );
  }

  // ───────────────────────────────────────────
  // private 메서드
  // ───────────────────────────────────────────

  private Comment findComment(UUID commentId) {
    return commentRepository.findById(commentId)
        .orElseThrow(CommentNotFoundException::new);
  }

  private CommentDto toDto(Comment comment) {
    return new CommentDto(
        comment.getId(),
        comment.getReviewId(),
        comment.getUserId(),
        null,        // userNickname → 나중에 User 조회 연동
        comment.getContent(),
        comment.getCreatedAt(),
        comment.getUpdatedAt()
    );
  }
}