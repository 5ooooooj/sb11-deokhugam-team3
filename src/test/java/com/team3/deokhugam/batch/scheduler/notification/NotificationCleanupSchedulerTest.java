package com.team3.deokhugam.batch.scheduler.notification;

import static org.mockito.Mockito.verify;

import com.team3.deokhugam.service.notification.NotificationCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupSchedulerTest {

  @Mock
  private NotificationCleanupService notificationCleanupService;

  @InjectMocks
  private NotificationCleanupScheduler notificationCleanupScheduler;

  @Test
  void deleteExpiredNotifications_callsService() {
    // when
    notificationCleanupScheduler.deleteExpiredNotifications();

    // then
    verify(notificationCleanupService).deleteExpiredNotifications();
  }
}
