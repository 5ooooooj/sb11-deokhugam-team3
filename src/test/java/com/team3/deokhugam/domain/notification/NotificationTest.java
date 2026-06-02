package com.team3.deokhugam.domain.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.exception.notification.NotificationForbiddenException;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class NotificationTest {

  @Test
  @DisplayName("알림 엔티티를 생성하면 필수 정보가 정상적으로 저장됩니다.")
  void createNotification() {
    // given
    User user = mock(User.class);
    Review review = mock(Review.class);
    NotificationType type = NotificationType.COMMENT;
    String message = "댓글이 달렸습니다.";

    // when
    Notification notification = Notification.create(user, review, type, message);

    // then
    assertThat(notification.getUser()).isEqualTo(user);
    assertThat(notification.getReview()).isEqualTo(review);
    assertThat(notification.getType()).isEqualTo(type);
    assertThat(notification.getMessage()).isEqualTo(message);
    assertThat(notification.isConfirmed()).isFalse();
  }

  @Test
  @DisplayName("알림 읽음 처리 시 confirmed가 true로 변경됩니다.")
  void confirmNotification() {
    // given
    Notification notification = Notification.create(
        mock(User.class), mock(Review.class),
        NotificationType.COMMENT, "메시지"
    );

    // when
    notification.confirm();

    // then
    assertThat(notification.isConfirmed()).isTrue();
  }

  @Test
  @DisplayName("본인 알림 소유자 검증에 성공합니다.")
  void validateOwner_success() {
    // given
    UUID userId = UUID.randomUUID();
    User user = mock(User.class);
    given(user.getId()).willReturn(userId);

    Notification notification = Notification.create(
        user, mock(Review.class),
        NotificationType.COMMENT, "메시지"
    );

    // when & then
    notification.validateOwner(userId); // 예외 없으면 성공
  }

  @Test
  @DisplayName("다른 사용자가 알림 소유자 검증 시 예외가 발생합니다.")
  void validateOwner_forbidden() {
    // given
    UUID userId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    User user = mock(User.class);
    given(user.getId()).willReturn(userId);

    Notification notification = Notification.create(
        user, mock(Review.class),
        NotificationType.COMMENT, "메시지"
    );

    // when & then
    assertThatThrownBy(() -> notification.validateOwner(otherUserId))
        .isInstanceOf(NotificationForbiddenException.class);
  }

  @Test
  @DisplayName("user가 null이면 알림 생성 시 예외가 발생합니다.")
  void createNotification_nullUser() {
    assertThatThrownBy(() ->
        Notification.create(null, mock(Review.class),
            NotificationType.COMMENT, "메시지"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  @DisplayName("message가 blank이면 알림 생성 시 예외가 발생합니다.")
  void createNotification_blankMessage() {
    assertThatThrownBy(() ->
        Notification.create(mock(User.class), mock(Review.class),
            NotificationType.COMMENT, ""))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() ->
        Notification.create(mock(User.class), mock(Review.class),
            NotificationType.COMMENT, "   "))
        .isInstanceOf(IllegalArgumentException.class);
  }
}