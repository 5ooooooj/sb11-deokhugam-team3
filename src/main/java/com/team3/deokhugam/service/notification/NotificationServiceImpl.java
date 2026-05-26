package com.team3.deokhugam.service.notification;

import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class NotificationServiceImpl implements NotificationService {

  @Override
  public void createCommentNotification(UUID reviewId, UUID commenterUserId) {
    // TODO: 나중에 구현
  }

  @Override
  public void createLikeNotification(UUID reviewId, UUID likerUserId) {
    // TODO: 나중에 구현
  }

  @Override
  public void createRankingNotification(UUID reviewId, String period) {
    // TODO: 나중에 구현
  }
}