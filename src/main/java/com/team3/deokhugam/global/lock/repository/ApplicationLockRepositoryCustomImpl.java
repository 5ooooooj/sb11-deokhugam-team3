package com.team3.deokhugam.global.lock.repository;

import static com.team3.deokhugam.global.lock.domain.QApplicationLockEntity.applicationLockEntity;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.team3.deokhugam.global.lock.domain.ApplicationLockEntity;
import com.team3.deokhugam.global.lock.domain.LockStatus;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class ApplicationLockRepositoryCustomImpl implements ApplicationLockRepositoryCustom {

  private final JPAQueryFactory queryFactory;

  public ApplicationLockRepositoryCustomImpl(EntityManager entityManager) {
    this.queryFactory = new JPAQueryFactory(entityManager);
  }

  // 특정 lockKey로 LOCKED상태인 락 하나 찾기
  @Override
  public Optional<ApplicationLockEntity> findLockedByLockKey(String lockKey) {
    ApplicationLockEntity result = queryFactory
        .selectFrom(applicationLockEntity)
        .where(
            applicationLockEntity.lockKey.eq(lockKey),
            applicationLockEntity.status.eq(LockStatus.LOCKED)
        )
        .fetchOne();

    return Optional.ofNullable(result);
  }

  // LOCKED 상태인데 expiresAt이 now보다 이전인 락들을 모두 찾기
  @Override
  public List<ApplicationLockEntity> findExpiredLocks(Instant now) {
    return queryFactory
        .selectFrom(applicationLockEntity)
        .where(
            applicationLockEntity.status.eq(LockStatus.LOCKED),
            applicationLockEntity.expiresAt.before(now)
        )
        .fetch();
  }
}