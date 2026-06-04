package com.team3.deokhugam.batch.scheduler.notification;

import com.team3.deokhugam.service.notification.NotificationCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationCleanupScheduler {

  private final NotificationCleanupService notificationCleanupService;

  // 매일 새벽 2시 실행 (윤선님 배치 00:00과 겹치지 않도록)
  @Scheduled(cron = "${notification.cleanup.cron:-}")
  public void deleteExpiredNotifications() {
    notificationCleanupService.deleteExpiredNotifications();
  }
}
