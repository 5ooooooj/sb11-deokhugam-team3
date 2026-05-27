package com.team3.deokhugam.repository.notification;

import com.team3.deokhugam.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  List<Notification> findByUserIdWithCursor(UUID userId, Instant after, Pageable pageable);

  void confirmAllByUserId(UUID userId);

  void deleteExpiredNotifications(Instant threshold);
}