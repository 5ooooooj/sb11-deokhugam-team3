package com.team3.deokhugam.batch.scheduler.comment;

import com.team3.deokhugam.service.comment.CommentCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentCleanupScheduler {

  private final CommentCleanupService commentCleanupService;

  // 매일 새벽 3시 실행 (윤선님 배치 00:00, Notification 02:00과 겹치지 않도록)
  @Scheduled(cron = "${comment.cleanup.cron:-}")
  public void deleteExpiredComments() {
    commentCleanupService.deleteExpiredComments();
  }
}
