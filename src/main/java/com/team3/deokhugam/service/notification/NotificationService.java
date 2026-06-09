package com.team3.deokhugam.service.notification;

import com.team3.deokhugam.dto.notification.NotificationDto;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import java.time.Instant;
import java.util.UUID;

public interface NotificationService {
  void createCommentNotification(UUID reviewId, UUID commenterUserId);
  void createLikeNotification(UUID reviewId, UUID likerUserId);
  void createRankingNotification(UUID reviewId, String period);

  NotificationDto confirm(UUID notificationId, UUID requestUserId);
  void confirmAll(UUID requestUserId);
  CursorPageResponse<NotificationDto> findAll(UUID userId, Instant after, int limit);
}