package com.team3.deokhugam.repository.comment;

import com.team3.deokhugam.domain.comment.Comment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  // after가 null이면 Instant.MAX를 sentinel로 사용하여 단일 쿼리로 처리
  // PostgreSQL null 파라미터 타입 추론 실패 문제 해결
  default List<Comment> findByReviewIdWithCursor(UUID reviewId, Instant after, Pageable pageable) {
    Instant cursor = after != null ? after : Instant.parse("9999-12-31T23:59:59Z");
    return findByReviewId(reviewId, cursor, pageable);
  }

  @Query("""
      SELECT c
      FROM Comment c
      WHERE c.review.id = :reviewId
        AND c.deletedAt IS NULL
        AND c.createdAt <= :cursor
      ORDER BY c.createdAt DESC, c.id DESC
      """)
  List<Comment> findByReviewId(
      @Param("reviewId") UUID reviewId,
      @Param("cursor") Instant cursor,
      Pageable pageable
  );

  // 영속성 컨텍스트와 DB 동기화: 호출 전/후 자동 flush/clear
  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("""
      DELETE FROM Comment c
      WHERE c.deletedAt IS NOT NULL
        AND c.deletedAt < :threshold
      """)
  void deleteExpiredComments(@Param("threshold") Instant threshold);
}
