package com.team3.deokhugam.repository.user;

import com.team3.deokhugam.domain.user.User;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface UserRepositoryCustom {
  Optional<User> findActiveByEmail(String email);

  Optional<User> findActiveById(UUID id);

  int deleteExpiredSoftDeletedUsers(Instant deleteBefore);

}
