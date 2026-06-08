package com.team3.deokhugam.service.notification;

import com.team3.deokhugam.domain.notification.Notification;
import com.team3.deokhugam.domain.notification.NotificationType;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.notification.NotificationDto;
import com.team3.deokhugam.exception.notification.NotificationNotFoundException;
import com.team3.deokhugam.exception.user.UserNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.notification.NotificationRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
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
public class NotificationServiceImpl implements NotificationService {

  private final NotificationRepository notificationRepository;
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;

  @Override
  public void createCommentNotification(UUID reviewId, UUID commenterUserId) {
    createNotification(reviewId, commenterUserId, NotificationType.COMMENT, "댓글이 달렸습니다.");
  }

  @Override
  public void createLikeNotification(UUID reviewId, UUID likerUserId) {
    createNotification(reviewId, likerUserId, NotificationType.LIKE, "좋아요가 달렸습니다.");
  }

  @Override
  public void createRankingNotification(UUID reviewId, String period) {
    createNotification(reviewId, null, NotificationType.POPULAR_REVIEW,
        "리뷰가 " + period + " TOP10에 선정되었습니다.");
  }

  @Override
  public NotificationDto confirm(UUID notificationId, UUID requestUserId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(NotificationNotFoundException::new);

    notification.validateOwner(requestUserId);
    notification.confirm();

    return toDto(notification);
  }

  @Override
  public void confirmAll(UUID requestUserId) {
    notificationRepository.confirmAllByUserId(requestUserId);
  }

  @Override
  @Transactional(readOnly = true)
  public CursorPageResponse<NotificationDto> findAll(UUID userId, Instant after, int size) {
    if (size < 1) {
      throw new IllegalArgumentException("size는 1 이상이어야 합니다.");
    }

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        userId, after, PageRequest.of(0, size + 1)
    );

    boolean hasNext = result.size() > size;
    List<Notification> content = hasNext
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

  private void createNotification(UUID reviewId, UUID actorUserId, NotificationType type, String message) {
    reviewRepository.findById(reviewId).ifPresent(review -> {
      // 본인 리뷰에 대한 알림은 생성하지 않음
      if (actorUserId != null && review.getUserId().equals(actorUserId)) {
        return;
      }
      userRepository.findById(review.getUserId()).ifPresent(user -> {
        Notification notification = Notification.create(
            user, review, type, message
        );
        notificationRepository.save(notification);
      });
    });
  }

  private NotificationDto toDto(Notification notification) {
    return new NotificationDto(
        notification.getId(),
        notification.getUser().getId(),
        notification.getReview() != null ? notification.getReview().getId() : null,
        null,
        notification.getMessage(),
        notification.getType(),
        notification.isConfirmed(),
        notification.getCreatedAt(),
        notification.getUpdatedAt()
    );
  }
}