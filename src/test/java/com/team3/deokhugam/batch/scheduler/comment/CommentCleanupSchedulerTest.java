package com.team3.deokhugam.batch.scheduler.comment;

import static org.mockito.Mockito.verify;

import com.team3.deokhugam.service.comment.CommentCleanupService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentCleanupSchedulerTest {

  @Mock
  private CommentCleanupService commentCleanupService;

  @InjectMocks
  private CommentCleanupScheduler commentCleanupScheduler;

  @Test
  void deleteExpiredComments_callsService() {
    // when
    commentCleanupScheduler.deleteExpiredComments();

    // then
    verify(commentCleanupService).deleteExpiredComments();
  }
}
