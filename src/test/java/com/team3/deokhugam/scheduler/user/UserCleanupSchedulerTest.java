package com.team3.deokhugam.scheduler.user;

import static org.mockito.Mockito.verify;

import com.team3.deokhugam.service.user.UserCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserCleanupSchedulerTest {

  @Mock
  private UserCleanupService userCleanupService;

  @InjectMocks
  private UserCleanupScheduler userCleanupScheduler;

  @Test
  void hardDeleteExpiredUsers_callsService() {
    // when
    userCleanupScheduler.hardDeleteExpiredUsers();

    // then
    verify(userCleanupService).hardDeleteExpiredUsers();
  }
}