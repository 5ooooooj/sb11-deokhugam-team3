package com.team3.deokhugam.repository.notification;

import static com.team3.deokhugam.domain.notification.NotificationTestFactory.notification;
import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.notification.Notification;
import com.team3.deokhugam.domain.notification.NotificationType;
import com.team3.deokhugam.global.config.JpaAuditingConfig;
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

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaAuditingConfig.class)
class NotificationRepositoryTest {

  @Autowired
  private NotificationRepository notificationRepository;

  @Test
  @DisplayName("알림을 저장하고 ID로 조회할 수 있습니다.")
  void saveAndFindById() {
    Notification n = notification().message("댓글이 달렸습니다.").build();

    Notification saved = notificationRepository.save(n);

    assertThat(notificationRepository.findById(saved.getId())).isPresent();
  }

  @Test
  @DisplayName("userId로 알림 목록을 조회할 수 있습니다.")
  void findByUserIdWithCursor_success() {
    Notification n1 = notification().message("댓글1").build();
    Notification n2 = notification().user(n1.getUser()).message("좋아요1").build();
    notificationRepository.saveAll(List.of(n1, n2));

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        n1.getUser().getId(), null, PageRequest.of(0, 10)
    );

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("limit 조건이 적용됩니다.")
  void findByUserIdWithCursor_withLimit() {
    Notification n1 = notification().message("댓글1").build();
    Notification n2 = notification().user(n1.getUser()).message("댓글2").build();
    Notification n3 = notification().user(n1.getUser()).message("댓글3").build();
    notificationRepository.saveAll(List.of(n1, n2, n3));

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        n1.getUser().getId(), null, PageRequest.of(0, 2)
    );

    assertThat(result).hasSize(2);
  }

  @Test
  @DisplayName("전체 읽음 처리가 동작합니다.")
  void confirmAllByUserId() {
    Notification n1 = notification().message("댓글1").build();
    Notification n2 = notification().user(n1.getUser()).message("좋아요1").build();
    notificationRepository.saveAll(List.of(n1, n2));

    notificationRepository.confirmAllByUserId(n1.getUser().getId());

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        n1.getUser().getId(), null, PageRequest.of(0, 10)
    );
    assertThat(result).allMatch(Notification::isConfirmed);
  }

  @Test
  @DisplayName("만료된 알림이 삭제됩니다.")
  void deleteExpiredNotifications() {
    Notification n1 = notification().message("댓글1").build();
    notificationRepository.save(n1);
    n1.confirm();
    notificationRepository.save(n1);

    notificationRepository.deleteExpiredNotifications(Instant.now().plusSeconds(1));

    List<Notification> result = notificationRepository.findByUserIdWithCursor(
        n1.getUser().getId(), null, PageRequest.of(0, 10)
    );
    assertThat(result).isEmpty();
  }
}