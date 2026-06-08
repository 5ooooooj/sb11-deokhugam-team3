package com.team3.deokhugam.repository.notification;

import com.team3.deokhugam.domain.notification.Notification;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  // after가 null이면 Instant.MAX를 sentinel로 사용하여 단일 쿼리로 처리
  // PostgreSQL null 파라미터 타입 추론 실패 문제 해결
  default List<Notification> findByUserIdWithCursor(UUID userId, Instant after, Pageable pageable) {
    Instant cursor = after != null ? after : Instant.parse("9999-12-31T23:59:59Z");
    return findByUserId(userId, cursor, pageable);
  }

  @Query("""
      SELECT n
      FROM Notification n
      WHERE n.user.id = :userId
        AND n.createdAt <= :cursor
      ORDER BY n.createdAt DESC, n.id DESC
      """)
  List<Notification> findByUserId(
      @Param("userId") UUID userId,
      @Param("cursor") Instant cursor,
      Pageable pageable
  );

  // 영속성 컨텍스트와 DB 동기화: 호출 전/후 자동 flush/clear
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE Notification n
      SET n.confirmed = true
      WHERE n.user.id = :userId
        AND n.confirmed = false
      """)
  void confirmAllByUserId(@Param("userId") UUID userId);

  // 영속성 컨텍스트와 DB 동기화: 호출 전/후 자동 flush/clear
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      DELETE FROM Notification n
      WHERE n.confirmed = true
        AND n.updatedAt < :threshold
      """)
  void deleteExpiredNotifications(@Param("threshold") Instant threshold);
}
