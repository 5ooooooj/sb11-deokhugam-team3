package com.team3.deokhugam.global.lock.service;

import com.team3.deokhugam.exception.lock.ApplicationLockAlreadyAcquiredException;
import com.team3.deokhugam.exception.lock.ApplicationLockNotFoundException;
import com.team3.deokhugam.global.lock.domain.ApplicationLockEntity;
import com.team3.deokhugam.global.lock.domain.LockName;
import com.team3.deokhugam.global.lock.domain.LockTarget;
import com.team3.deokhugam.global.lock.repository.ApplicationLockRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ApplicationLockService {

  private final ApplicationLockRepository applicationLockRepository;

  @Transactional
  public UUID acquire(
      LockTarget target,
      String targetId,
      LockName lockName,
      // 락 시간, 어노테이션 달때 정함
      Duration duration
  ){
    // USER:test@test.com:USER_REGISTER
    String lockKey = createLockKey(target, targetId, lockName);

    // LOCKED상태인 lockKey있을 시 예외 반환 (RELEASED일땐 정상 실행)
    applicationLockRepository.findLockedByLockKey(lockKey)
        .ifPresent(lock -> {
          throw new ApplicationLockAlreadyAcquiredException();
        });

    Instant lockedAt = Instant.now();
    Instant expiresAt = lockedAt.plus(duration);

    ApplicationLockEntity lock = ApplicationLockEntity.lock(
        target,
        targetId,
        lockName,
        lockKey,
        lockedAt,
        expiresAt
    );

    ApplicationLockEntity savedLock = applicationLockRepository.saveAndFlush(lock);
    return savedLock.getId();
  }

  @Transactional
  public void release(UUID lockId) {
    ApplicationLockEntity lock = applicationLockRepository.findById(lockId)
        .orElseThrow(ApplicationLockNotFoundException::new);

    lock.release();
  }

  // 방어 배치 호출 시 만료 락 해제용
  @Transactional
  public int releaseExpiredLocks() {
    Instant now = Instant.now();

    List<ApplicationLockEntity> expiredLocks =
        applicationLockRepository.findExpiredLocks(now);

    for (ApplicationLockEntity lock : expiredLocks) {
      lock.release();
    }

    return expiredLocks.size();
  }

  private String createLockKey(
      LockTarget target,
      String targetId,
      LockName lockName
  ) {
    return target + ":" + targetId + ":" + lockName;
  }
}
