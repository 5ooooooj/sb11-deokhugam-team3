package com.team3.deokhugam.repository.notification;

import com.team3.deokhugam.domain.notification.Notification;
import com.team3.deokhugam.domain.notification.NotificationType;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  default List<Notification> findByUserIdWithCursor(UUID userId, Instant after, Pageable pageable) {
    if (after == null) {
      return findByUserIdWithoutCursor(userId, pageable);
    }
    return findByUserIdWithAfter(userId, after, pageable);
  }

  @Query("""
      SELECT n
      FROM Notification n
      WHERE n.user.id = :userId
      ORDER BY n.createdAt DESC
      """)
  List<Notification> findByUserIdWithoutCursor(
      @Param("userId") UUID userId,
      Pageable pageable
  );

  @Query("""
      SELECT n
      FROM Notification n
      WHERE n.user.id = :userId
        AND n.createdAt < :after
      ORDER BY n.createdAt DESC
      """)
  List<Notification> findByUserIdWithAfter(
      @Param("userId") UUID userId,
      @Param("after") Instant after,
      Pageable pageable
  );

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      UPDATE Notification n
      SET n.confirmed = true
      WHERE n.user.id = :userId
        AND n.confirmed = false
      """)
  void confirmAllByUserId(@Param("userId") UUID userId);

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      DELETE FROM Notification n
      WHERE n.confirmed = true
        AND n.updatedAt < :threshold
      """)
  void deleteExpiredNotifications(@Param("threshold") Instant threshold);

  boolean existsByReviewIdAndTypeAndMessage(UUID reviewId, NotificationType type, String message);
}
