package com.team3.deokhugam.service.notification;

import com.team3.deokhugam.repository.notification.NotificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationCleanupService {

  private final NotificationRepository notificationRepository;

  @Transactional
  public void deleteExpiredNotifications() {
    Instant threshold = Instant.now().minus(7, ChronoUnit.DAYS);
    notificationRepository.deleteExpiredNotifications(threshold);
  }
}
