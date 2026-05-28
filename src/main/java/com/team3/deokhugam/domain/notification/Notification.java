package com.team3.deokhugam.domain.notification;

import com.team3.deokhugam.domain.base.BaseEntity;
import com.team3.deokhugam.exception.notification.NotificationForbiddenException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

  @Column(name = "user_id", nullable = false, updatable = false)
  private UUID userId;

  @Column(name = "review_id", nullable = false, updatable = false)
  private UUID reviewId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = true)
  private NotificationType type;

  @Column(nullable = false)
  private String message;

  @Column(nullable = false)
  private boolean confirmed = false;

  public static Notification create(UUID userId, UUID reviewId,
      NotificationType type, String message) {
    if (userId == null || reviewId == null || message == null) {
      throw new IllegalArgumentException("userId, reviewId, message must not be null");
    }
    Notification notification = new Notification();
    notification.userId = userId;
    notification.reviewId = reviewId;
    notification.type = type;
    notification.message = message;
    return notification;
  }

  // 읽음 처리
  public void confirm() {
    this.confirmed = true;
  }

  // 본인 확인
  public void validateOwner(UUID requestUserId) {
    if (!this.userId.equals(requestUserId)) {
      throw new NotificationForbiddenException();
    }
  }
}