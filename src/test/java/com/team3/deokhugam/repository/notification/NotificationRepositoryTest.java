package com.team3.deokhugam.repository.notification;

import static com.team3.deokhugam.domain.notification.NotificationTestFactory.notification;
import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.notification.Notification;
import com.team3.deokhugam.domain.notification.NotificationType;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewTestFactory;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.repository.BaseRepositoryTest;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

class NotificationRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Test
  @DisplayName("알림을 저장하고 ID로 조회할 수 있습니다.")
  void saveAndFindById() {
    User user = userRepository.save(new User("test1@test.com", "테스터1", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    Notification saved = notificationRepository.save(
        notification().user(user).review(review).message("댓글이 달렸습니다.").build()
    );

    assertThat(notificationRepository.findById(saved.getId())).isPresent();
  }

  @Test
  @DisplayName("userId로 알림 목록을 조회할 수 있습니다.")
  void findByUserIdWithCursor_success() {
    User user = userRepository.save(new User("test2@test.com", "테스터2", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    notificationRepository.saveAll(List.of(
        notification().user(user).review(review).type(NotificationType.COMMENT).message("댓글1").build(),
        notification().user(user).review(review).type(NotificationType.LIKE).message("좋아요1").build()
    ));

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), null, PageRequest.of(0, 10)
    );

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("limit 조건이 적용됩니다.")
  void findByUserIdWithCursor_withLimit() {
    User user = userRepository.save(new User("test3@test.com", "테스터3", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    notificationRepository.saveAll(List.of(
        notification().user(user).review(review).message("댓글1").build(),
        notification().user(user).review(review).message("댓글2").build(),
        notification().user(user).review(review).message("댓글3").build()
    ));

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), null, PageRequest.of(0, 2)
    );

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("전체 읽음 처리가 동작합니다.")
  void confirmAllByUserId() {
    User user = userRepository.save(new User("test4@test.com", "테스터4", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    notificationRepository.saveAll(List.of(
        notification().user(user).review(review).type(NotificationType.COMMENT).message("댓글1").build(),
        notification().user(user).review(review).type(NotificationType.LIKE).message("좋아요1").build()
    ));

    notificationRepository.confirmAllByUserId(user.getId());

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), null, PageRequest.of(0, 10)
    );
    assertThat(result).allMatch(Notification::isConfirmed);
  }

  @Test
  @DisplayName("만료된 알림이 삭제됩니다.")
  void deleteExpiredNotifications() {
    User user = userRepository.save(new User("test5@test.com", "테스터5", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    Notification n1 = notificationRepository.save(
        notification().user(user).review(review).message("댓글1").build()
    );
    n1.confirm();
    notificationRepository.save(n1);

    notificationRepository.deleteExpiredNotifications(Instant.now().plusSeconds(1));

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), null, PageRequest.of(0, 10)
    );
    assertThat(result).isEmpty();
  }
}