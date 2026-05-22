package com.team3.deokhugam.domain.notification.service;

import java.util.UUID;

public interface NotificationService {
  void createCommentNotification(UUID reviewId, UUID commenterUserId);
  void createLikeNotification(UUID reviewId, UUID likerUserId);
  void createRankingNotification(UUID reviewId, String period);

}
