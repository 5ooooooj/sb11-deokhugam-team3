package com.team3.deokhugam.service.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.repository.notification.NotificationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationCleanupServiceTest {

  @Mock
  private NotificationRepository notificationRepository;

  @InjectMocks
  private NotificationCleanupService notificationCleanupService;

  @Test
  void deleteExpiredNotifications_callsRepository() {
    // when
    notificationCleanupService.deleteExpiredNotifications();

    // then
    verify(notificationRepository).deleteExpiredNotifications(any());
  }
}
