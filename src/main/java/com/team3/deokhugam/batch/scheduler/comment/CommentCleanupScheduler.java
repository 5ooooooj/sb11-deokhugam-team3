package com.team3.deokhugam.batch.scheduler.comment;

import com.team3.deokhugam.service.comment.CommentCleanupService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CommentCleanupScheduler {

  private final CommentCleanupService commentCleanupService;

// 매일 새벽 3시 실행 (comment.cleanup.cron 설정 필요, 미설정 시 비활성화)
// 윤선님 배치 00:00, Notification 02:00과 겹치지 않도록 새벽 3시 권장
  @Scheduled(cron = "${comment.cleanup.cron:-}")
  public void deleteExpiredComments() {
    commentCleanupService.deleteExpiredComments();
  }
}
