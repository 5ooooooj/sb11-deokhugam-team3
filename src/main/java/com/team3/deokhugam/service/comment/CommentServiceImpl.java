package com.team3.deokhugam.service.comment;

import com.team3.deokhugam.domain.comment.Comment;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.comment.CommentCreateRequest;
import com.team3.deokhugam.dto.comment.CommentDto;
import com.team3.deokhugam.dto.comment.CommentUpdateRequest;
import com.team3.deokhugam.exception.comment.CommentNotFoundException;
import com.team3.deokhugam.exception.review.ReviewNotFoundException;
import com.team3.deokhugam.exception.user.UserNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.comment.CommentRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
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
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  @Override
  public CommentDto create(CommentCreateRequest request) {
    Review review = reviewRepository.findByIdAndDeletedAtIsNull(request.reviewId())
        .orElseThrow(ReviewNotFoundException::new);
    User user = userRepository.findActiveById(request.userId())
        .orElseThrow(UserNotFoundException::new);

    Comment comment = Comment.create(review, user, request.content());
    commentRepository.save(comment);

    notificationService.createCommentNotification(
        request.reviewId(), request.userId()
    );

    return toDto(comment);
  }

  @Override
  public CommentDto update(UUID commentId, UUID requestUserId,
      CommentUpdateRequest request) {
    Comment comment = findComment(commentId);

    if (comment.isDeleted()) {
      throw new CommentNotFoundException();
    }

    comment.validateOwner(requestUserId);
    comment.updateContent(request.content());
    return toDto(comment);
  }

  @Override
  public void delete(UUID commentId, UUID requestUserId) {
    Comment comment = findComment(commentId);

    if (comment.isDeleted()) {
      throw new CommentNotFoundException();
    }

    comment.validateOwner(requestUserId);
    comment.softDelete();
  }

  @Override
  public void hardDelete(UUID commentId, UUID requestUserId) {
    Comment comment = findComment(commentId);
    comment.validateOwner(requestUserId);
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
        comment.getReview().getId(),
        comment.getUser() != null ? comment.getUser().getId() : null,
        comment.getUser() != null ? comment.getUser().getNickname() : "탈퇴한 사용자",
        comment.getContent(),
        comment.getCreatedAt(),
        comment.getUpdatedAt()
    );
  }
}