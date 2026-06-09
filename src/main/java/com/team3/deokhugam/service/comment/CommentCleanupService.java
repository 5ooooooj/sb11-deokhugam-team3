package com.team3.deokhugam.service.comment;

import com.team3.deokhugam.repository.comment.CommentRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentCleanupService {

  private final CommentRepository commentRepository;

  @Transactional
  public void deleteExpiredComments() {
    Instant threshold = Instant.now().minus(30, ChronoUnit.DAYS);
    commentRepository.deleteExpiredComments(threshold);
  }
}
