package com.team3.deokhugam.service.user;

import com.team3.deokhugam.repository.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserCleanupService {

  private final UserRepository userRepository;

  @Transactional
  public int hardDeleteExpiredUsers(){
    Instant deleteBefore = Instant.now().minus(1, ChronoUnit.DAYS);
    return userRepository.deleteExpiredSoftDeletedUsers(deleteBefore);
  }

}
