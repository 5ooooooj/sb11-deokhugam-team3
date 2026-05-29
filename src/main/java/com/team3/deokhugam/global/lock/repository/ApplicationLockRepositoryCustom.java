package com.team3.deokhugam.global.lock.repository;

import com.team3.deokhugam.global.lock.domain.ApplicationLockEntity;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ApplicationLockRepositoryCustom {

  // 현재 LOCKED 상태인 락 조회
  // 락 생성전 중복 확인용
  Optional<ApplicationLockEntity> findLockedByLockKey(String lockKey);

  // LOCKED상태인데 expiresAt이 now보다 이전인 락 조회
  // 방어 배치용
  List<ApplicationLockEntity> findExpiredLocks(Instant now);

}
