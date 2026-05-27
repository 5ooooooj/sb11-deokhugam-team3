package com.team3.deokhugam.repository.notification;

import com.team3.deokhugam.domain.notification.Notification;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  @Query("""
        SELECT n FROM Notification n
        WHERE n.userId = :userId
        AND (:after IS NULL OR n.createdAt < :after)
        ORDER BY n.createdAt DESC
        """)
  List<Notification> findByUserIdWithCursor(
      @Param("userId") UUID userId,
      @Param("after") Instant after,
      Pageable pageable
  );

  @Modifying
  @Query("""
        UPDATE Notification n
        SET n.confirmed = true
        WHERE n.userId = :userId
        AND n.confirmed = false
        """)
  void confirmAllByUserId(@Param("userId") UUID userId);

  @Modifying
  @Query("""
        DELETE FROM Notification n
        WHERE n.confirmed = true
        AND n.updatedAt < :threshold
        """)
  void deleteExpiredNotifications(@Param("threshold") Instant threshold);
}