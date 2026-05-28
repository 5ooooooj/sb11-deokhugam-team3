package com.team3.deokhugam.scheduler.user;

import com.team3.deokhugam.service.user.UserCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class UserCleanupScheduler {

  private final UserCleanupService userCleanupService;

  @Scheduled(cron = "${user.cleanup.cron:-}")
  public void hardDeleteExpiredUsers(){
    userCleanupService.hardDeleteExpiredUsers();
  }
}
