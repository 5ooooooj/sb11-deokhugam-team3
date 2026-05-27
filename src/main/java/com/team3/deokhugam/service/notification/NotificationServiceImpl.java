package com.team3.deokhugam.service.notification;

import com.team3.deokhugam.dto.notification.NotificationDto;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import org.springframework.stereotype.Service;

import java.time.Instant;
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

  @Override
  public NotificationDto confirm(UUID notificationId, UUID requestUserId) {
    // TODO: 나중에 구현
    return null;
  }

  @Override
  public void confirmAll(UUID requestUserId) {
    // TODO: 나중에 구현
  }

  @Override
  public CursorPageResponse<NotificationDto> findAll(UUID userId, Instant after, int size) {
    // TODO: 나중에 구현
    return null;
  }
}