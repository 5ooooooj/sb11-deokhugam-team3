package com.team3.deokhugam.service.comment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.repository.comment.CommentRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommentCleanupServiceTest {

  @Mock
  private CommentRepository commentRepository;

  @InjectMocks
  private CommentCleanupService commentCleanupService;

  @Test
  void deleteExpiredComments_callsRepository() {
    Instant beforeCall = Instant.now();

    // when
    commentCleanupService.deleteExpiredComments();

    Instant afterCall = Instant.now();

    // then
    ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
    verify(commentRepository).deleteExpiredComments(captor.capture());

    Instant captured = captor.getValue();
    assertThat(captured).isBetween(
        beforeCall.minus(30, ChronoUnit.DAYS),
        afterCall.minus(30, ChronoUnit.DAYS)
    );
  }
}
