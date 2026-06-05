package com.team3.deokhugam.domain.notification;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewTestFactory;
import com.team3.deokhugam.domain.user.User;
import org.springframework.test.util.ReflectionTestUtils;

public class NotificationTestFactory {

  private User user = new User("test@test.com", "테스터", "Password1!");
  private Review review = ReviewTestFactory.review().build();
  private NotificationType type = NotificationType.COMMENT;
  private String message = "테스트 알림 메시지";

  private NotificationTestFactory() {}

  public static NotificationTestFactory notification() {
    return new NotificationTestFactory();
  }

  public NotificationTestFactory user(User user) {
    this.user = user;
    return this;
  }

  public NotificationTestFactory review(Review review) {
    this.review = review;
    return this;
  }

  public NotificationTestFactory type(NotificationType type) {
    this.type = type;
    return this;
  }

  public NotificationTestFactory message(String message) {
    this.message = message;
    return this;
  }

  public Notification build() {
    Notification notification = new Notification();
    ReflectionTestUtils.setField(notification, "user", user);
    ReflectionTestUtils.setField(notification, "review", review);
    ReflectionTestUtils.setField(notification, "type", type);
    ReflectionTestUtils.setField(notification, "message", message);
    ReflectionTestUtils.setField(notification, "confirmed", false);
    return notification;
  }
}