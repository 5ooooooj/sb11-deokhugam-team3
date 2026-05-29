package com.team3.deokhugam.global.lock.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "application_locks")
public class ApplicationLockEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  // 락 대상 도메인 (USER, BOOK, REVIEW, BATCH)
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LockTarget target;

  // 락 대상의 식별자
  // 예 : 회원가입 시 이메일 (test@test.com)
  // 이 값이 달라야 같은 작업도 서로 다른 대상으로 보고 동시 실행 가능
  @Column(name = "target_id", nullable = false)
  private String targetId;

  // 락 작업 종류 (USER_REGISTER, BOOK_CREATE)
  @Enumerated(EnumType.STRING)
  @Column(name = "lock_name", nullable = false)
  private LockName lockName;

  // 중복 락 방지를 위한 최종 식별 키 (target:targetId:lockName)
  @Column(name = "lock_key", nullable = false)
  private String lockKey;

  // LOCKED, RELEASED
  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private LockStatus status;

  // LOCKED_AT (락걸린 시간)
  @Column(name = "locked_at", nullable = false)
  private Instant lockedAt;

  // RELEASED_AT (락풀린 시간)
  @Column(name = "released_at")
  private Instant releasedAt;

  // EXPIRES_AT (락만료 시간)
  // 이 시간 이후에 남아있는 락은 비정상 (방어 배치용)
  @Column(name = "expires_at", nullable = false)
  private Instant expiresAt;

  private ApplicationLockEntity(
      LockTarget target,
      String targetId,
      LockName lockName,
      String lockKey,
      Instant lockedAt,
      Instant expiresAt
  ) {
    this.target = target;
    this.targetId = targetId;
    this.lockName = lockName;
    this.lockKey = lockKey;
    // 생성시 기본을 LOCKED로 설정
    this.status = LockStatus.LOCKED;
    this.lockedAt = lockedAt;
    this.expiresAt = expiresAt;
  }

  // 락 생성
  public static ApplicationLockEntity lock(
      LockTarget target,
      String targetId,
      LockName lockName,
      String lockKey,
      Instant lockedAt,
      Instant expiresAt
  ) {
    return new ApplicationLockEntity(
        target,
        targetId,
        lockName,
        lockKey,
        lockedAt,
        expiresAt);
  }

  // 락 해제
  public void release() {
    if (this.status == LockStatus.RELEASED) {
      return;
    }

    this.status = LockStatus.RELEASED;
    this.releasedAt = Instant.now();
  }

}
