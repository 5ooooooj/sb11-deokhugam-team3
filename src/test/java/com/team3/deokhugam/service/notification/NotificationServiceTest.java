package com.team3.deokhugam.service.notification;

import com.team3.deokhugam.domain.notification.Notification;
import com.team3.deokhugam.domain.notification.NotificationType;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.notification.NotificationDto;
import com.team3.deokhugam.exception.notification.NotificationForbiddenException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.notification.NotificationRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class NotificationServiceTest {

  @Mock
  private NotificationRepository notificationRepository;
  @Mock
  private ReviewRepository reviewRepository;
  @Mock
  private UserRepository userRepository;
  @InjectMocks
  private NotificationServiceImpl notificationService;

  @Test
  @DisplayName("댓글 알림 생성 성공")
  void createCommentNotification_success() {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID commenterUserId = UUID.randomUUID();
    UUID reviewOwnerId = UUID.randomUUID();

    Review review = mock(Review.class);
    User user = mock(User.class);
    given(review.getUserId()).willReturn(reviewOwnerId);
    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(userRepository.findById(reviewOwnerId)).willReturn(Optional.of(user));

    // when
    notificationService.createCommentNotification(reviewId, commenterUserId);

    // then
    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  @DisplayName("좋아요 알림 생성 성공")
  void createLikeNotification_success() {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID likerUserId = UUID.randomUUID();
    UUID reviewOwnerId = UUID.randomUUID();

    Review review = mock(Review.class);
    User user = mock(User.class);
    given(review.getUserId()).willReturn(reviewOwnerId);
    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(userRepository.findById(reviewOwnerId)).willReturn(Optional.of(user));

    // when
    notificationService.createLikeNotification(reviewId, likerUserId);

    // then
    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  @DisplayName("랭킹 알림 생성 성공")
  void createRankingNotification_success() {
    // given
    UUID reviewId = UUID.randomUUID();
    String period = "DAILY";
    UUID reviewOwnerId = UUID.randomUUID();

    Review review = mock(Review.class);
    User user = mock(User.class);
    given(review.getUserId()).willReturn(reviewOwnerId);
    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(userRepository.findById(reviewOwnerId)).willReturn(Optional.of(user));

    // when
    notificationService.createRankingNotification(reviewId, period);

    // then
    verify(notificationRepository).save(any(Notification.class));
  }

  @Test
  @DisplayName("단건 읽음 처리 성공")
  void confirm_success() {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    User mockUser = mock(User.class);
    Review mockReview = mock(Review.class);
    given(mockUser.getId()).willReturn(userId);
    given(mockReview.getId()).willReturn(UUID.randomUUID());

    Notification notification = mock(Notification.class);
    given(notification.getUser()).willReturn(mockUser);
    given(notification.getReview()).willReturn(mockReview);
    given(notificationRepository.findById(notificationId))
        .willReturn(Optional.of(notification));

    // when
    notificationService.confirm(notificationId, userId);

    // then
    verify(notification).confirm();
  }

  @Test
  @DisplayName("타인 알림 읽음 처리 시 예외")
  void confirm_forbidden() {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID otherId = UUID.randomUUID();

    Notification notification = mock(Notification.class);
    given(notificationRepository.findById(notificationId))
        .willReturn(Optional.of(notification));
    willThrow(new NotificationForbiddenException())
        .given(notification).validateOwner(otherId);

    // when & then
    assertThatThrownBy(() ->
        notificationService.confirm(notificationId, otherId))
        .isInstanceOf(NotificationForbiddenException.class);
  }

  @Test
  @DisplayName("전체 읽음 처리 성공")
  void confirmAll_success() {
    // given
    UUID userId = UUID.randomUUID();

    // when
    notificationService.confirmAll(userId);

    // then
    verify(notificationRepository).confirmAllByUserId(userId);
  }

  @Test
  @DisplayName("알림 목록 조회 성공")
  void findAll_success() {
    // given
    UUID userId = UUID.randomUUID();

    User mockUser = mock(User.class);
    Review mockReview = mock(Review.class);
    given(mockUser.getId()).willReturn(UUID.randomUUID());
    given(mockReview.getId()).willReturn(UUID.randomUUID());

    Notification notification1 = mock(Notification.class);
    Notification notification2 = mock(Notification.class);
    given(notification1.getUser()).willReturn(mockUser);
    given(notification1.getReview()).willReturn(mockReview);
    given(notification2.getUser()).willReturn(mockUser);
    given(notification2.getReview()).willReturn(mockReview);

    List<Notification> notifications = List.of(notification1, notification2);
    given(notificationRepository.findByUserIdWithCursor(
        eq(userId), isNull(), any())
    ).willReturn(notifications);

    // when
    CursorPageResponse<NotificationDto> result =
        notificationService.findAll(userId, null, 10);

    // then
    assertThat(result.hasNext()).isFalse();
    assertThat(result.content()).hasSize(2);
  }
  @Test
  @DisplayName("존재하지 않는 리뷰 알림 생성 시 저장 안 함")
  void createCommentNotification_reviewNotFound() {
    // given
    UUID reviewId = UUID.randomUUID();
    UUID commenterUserId = UUID.randomUUID();

    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    // when
    notificationService.createCommentNotification(reviewId, commenterUserId);

    // then
    verify(notificationRepository, never()).save(any());
  }
}