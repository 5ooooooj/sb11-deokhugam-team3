package com.team3.deokhugam.domain.notification;

import com.team3.deokhugam.domain.base.BaseEntity;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
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

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false, updatable = false)
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "review_id", updatable = false)
  private Review review;

  @Enumerated(EnumType.STRING)
  @Column(nullable = true)
  private NotificationType type;

  @Column(nullable = false)
  private String message;

  @Column(nullable = false)
  private boolean confirmed = false;

  // 정적 팩토리 메서드
  public static Notification create(User user, Review review,
      NotificationType type, String message) {
    if (user == null || review == null || type == null || message == null || message.isBlank()) {
      throw new IllegalArgumentException("user, review, type, message must not be null or blank");
    }
    Notification notification = new Notification();
    notification.user = user;
    notification.review = review;
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
    if (!this.user.getId().equals(requestUserId)) {
      throw new NotificationForbiddenException();
    }
  }
}