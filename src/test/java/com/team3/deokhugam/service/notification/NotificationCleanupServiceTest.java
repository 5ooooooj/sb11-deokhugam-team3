package com.team3.deokhugam.service.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.repository.notification.NotificationRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
    Instant beforeCall = Instant.now();

    // when
    notificationCleanupService.deleteExpiredNotifications();

    Instant afterCall = Instant.now();

    // then
    ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
    verify(notificationRepository).deleteExpiredNotifications(captor.capture());

    Instant captured = captor.getValue();
    assertThat(captured).isBetween(
        beforeCall.minus(30, ChronoUnit.DAYS),
        afterCall.minus(30, ChronoUnit.DAYS)
    );
  }
}