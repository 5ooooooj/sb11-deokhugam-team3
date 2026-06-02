package com.team3.deokhugam.repository.notification;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.notification.Notification;
import com.team3.deokhugam.domain.notification.NotificationType;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.global.config.JpaAuditingConfig;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;

import org.springframework.test.annotation.DirtiesContext;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_CLASS)  // ← 추가
class NotificationRepositoryTest {

  @Autowired
  private NotificationRepository notificationRepository;

  @Autowired
  private TestEntityManager entityManager;

  @Test
  @DisplayName("알림을 저장하고 ID로 조회할 수 있습니다.")
  void saveAndFindById() {
    // given
    User user = new User("test1@test.com", "테스터1", "Password1!");
    entityManager.persist(user);
    Review review = Review.create(user.getId(), UUID.randomUUID(), 5, "좋은 책이에요");
    entityManager.persist(review);
    entityManager.flush();

    Notification notification = Notification.create(
        user, review, NotificationType.COMMENT, "댓글이 달렸습니다."
    );

    // when
    Notification saved = notificationRepository.save(notification);

    // then
    assertThat(notificationRepository.findById(saved.getId())).isPresent();
  }

  @Test
  @DisplayName("userId로 알림 목록을 조회할 수 있습니다.")
  void findByUserIdWithCursor_success() {
    // given
    User user = new User("test2@test.com", "테스터2", "Password1!");
    entityManager.persist(user);
    Review review = Review.create(user.getId(), UUID.randomUUID(), 5, "좋은 책이에요");
    entityManager.persist(review);
    entityManager.flush();

    Notification n1 = Notification.create(user, review, NotificationType.COMMENT, "댓글1");
    Notification n2 = Notification.create(user, review, NotificationType.LIKE, "좋아요1");
    notificationRepository.saveAll(List.of(n1, n2));
    entityManager.flush();

    // when
    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), null, PageRequest.of(0, 10)
    );

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("limit 조건이 적용됩니다.")
  void findByUserIdWithCursor_withLimit() {
    // given
    User user = new User("test3@test.com", "테스터3", "Password1!");
    entityManager.persist(user);
    Review review = Review.create(user.getId(), UUID.randomUUID(), 5, "좋은 책이에요");
    entityManager.persist(review);
    entityManager.flush();

    notificationRepository.saveAll(List.of(
        Notification.create(user, review, NotificationType.COMMENT, "댓글1"),
        Notification.create(user, review, NotificationType.COMMENT, "댓글2"),
        Notification.create(user, review, NotificationType.COMMENT, "댓글3")
    ));
    entityManager.flush();

    // when
    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), null, PageRequest.of(0, 2)
    );

    // then
    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("전체 읽음 처리가 동작합니다.")
  void confirmAllByUserId() {
    // given
    User user = new User("test4@test.com", "테스터4", "Password1!");
    entityManager.persist(user);
    Review review = Review.create(user.getId(), UUID.randomUUID(), 5, "좋은 책이에요");
    entityManager.persist(review);
    entityManager.flush();

    Notification n1 = Notification.create(user, review, NotificationType.COMMENT, "댓글1");
    Notification n2 = Notification.create(user, review, NotificationType.LIKE, "좋아요1");
    notificationRepository.saveAll(List.of(n1, n2));
    entityManager.flush();

    // when
    notificationRepository.confirmAllByUserId(user.getId());
    entityManager.flush();
    entityManager.clear();

    // then
    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), null, PageRequest.of(0, 10)
    );
    assertThat(result).allMatch(Notification::isConfirmed);
  }

  @Test
  @DisplayName("만료된 알림이 삭제됩니다.")
  void deleteExpiredNotifications() {
    // given
    User user = new User("test5@test.com", "테스터5", "Password1!");
    entityManager.persist(user);
    Review review = Review.create(user.getId(), UUID.randomUUID(), 5, "좋은 책이에요");
    entityManager.persist(review);
    entityManager.flush();

    Notification n1 = Notification.create(user, review, NotificationType.COMMENT, "댓글1");
    notificationRepository.save(n1);
    n1.confirm();
    notificationRepository.save(n1);
    entityManager.flush();

    // when
    notificationRepository.deleteExpiredNotifications(Instant.now().plusSeconds(1));
    entityManager.flush();
    entityManager.clear();

    // then
    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        user.getId(), null, PageRequest.of(0, 10)
    );
    assertThat(result).isEmpty();
  }
}