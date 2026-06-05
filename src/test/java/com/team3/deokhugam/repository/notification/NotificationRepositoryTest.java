package com.team3.deokhugam.repository.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.notification.Notification;
import com.team3.deokhugam.domain.notification.NotificationType;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewTestFactory;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.global.config.JpaAuditingConfig;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class NotificationRepositoryTest {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private ReviewRepository reviewRepository;

  @Autowired
  private jakarta.persistence.EntityManager entityManager;


  @Test
  @DisplayName("알림을 저장하고 ID로 조회할 수 있습니다.")
  void saveAndFindById() {
    User user = userRepository.save(new User("test1@test.com", "테스터1", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    Notification saved = notificationRepository.save(
        Notification.create(user, review, NotificationType.COMMENT, "댓글이 달렸습니다.")
    );

    assertThat(notificationRepository.findById(saved.getId())).isPresent();
  }

  @Test
  @DisplayName("userId로 알림 목록을 조회할 수 있습니다.")
  void findByUserIdWithCursor_success() {
    User user = userRepository.save(new User("test2@test.com", "테스터2", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    notificationRepository.saveAll(List.of(
        Notification.create(user, review, NotificationType.COMMENT, "댓글1"),
        Notification.create(user, review, NotificationType.LIKE, "좋아요1")
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
        Notification.create(user, review, NotificationType.COMMENT, "댓글1"),
        Notification.create(user, review, NotificationType.COMMENT, "댓글2"),
        Notification.create(user, review, NotificationType.COMMENT, "댓글3")
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
        Notification.create(user, review, NotificationType.COMMENT, "댓글1"),
        Notification.create(user, review, NotificationType.LIKE, "좋아요1")
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

    Notification n1 = Notification.create(user, review, NotificationType.COMMENT, "댓글1");
    n1.confirm();
    Notification saved = notificationRepository.save(n1);

    // updatedAt 직접 업데이트
    entityManager.createQuery(
            "UPDATE Notification n SET n.updatedAt = :updatedAt WHERE n.id = :id")
        .setParameter("updatedAt", Instant.parse("2024-01-01T00:00:00Z"))
        .setParameter("id", saved.getId())
        .executeUpdate();

    entityManager.flush();
    entityManager.clear();

    notificationRepository.deleteExpiredNotifications(Instant.parse("2024-06-01T00:00:00Z"));

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), null, PageRequest.of(0, 10)
    );
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("after 파라미터로 커서 페이지네이션이 동작합니다.")
  void findByUserIdWithCursor_withAfter() {
    User user = userRepository.save(new User("test6@test.com", "테스터6", "Password1!"));
    Review review = reviewRepository.save(ReviewTestFactory.review().userId(user.getId()).build());

    Notification n1 = notificationRepository.save(
        Notification.create(user, review, NotificationType.COMMENT, "첫 번째 알림")
    );
    Notification n2 = notificationRepository.save(
        Notification.create(user, review, NotificationType.COMMENT, "두 번째 알림")
    );

    // createdAt 직접 업데이트
    entityManager.createQuery(
            "UPDATE Notification n SET n.createdAt = :createdAt WHERE n.id = :id")
        .setParameter("createdAt", Instant.parse("2024-01-01T00:00:00Z"))
        .setParameter("id", n1.getId())
        .executeUpdate();

    entityManager.createQuery(
            "UPDATE Notification n SET n.createdAt = :createdAt WHERE n.id = :id")
        .setParameter("createdAt", Instant.parse("2024-01-02T00:00:00Z"))
        .setParameter("id", n2.getId())
        .executeUpdate();

    entityManager.flush();
    entityManager.clear();

    Instant after = Instant.parse("2024-01-01T12:00:00Z");

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), after, PageRequest.of(0, 10)
    );

    assertThat(result).hasSize(1);
    assertThat(result.get(0).getMessage()).isEqualTo("첫 번째 알림");
  }
}