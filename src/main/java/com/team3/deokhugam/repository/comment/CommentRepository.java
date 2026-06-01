package com.team3.deokhugam.repository.comment;

import com.team3.deokhugam.domain.comment.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  @Query("""
        SELECT c FROM Comment c
        WHERE c.review.id = :reviewId
        AND c.deletedAt IS NULL
        AND (:after IS NULL OR c.createdAt < :after)
        ORDER BY c.createdAt DESC
        """)
  List<Comment> findByReviewIdWithCursor(
      @Param("reviewId") UUID reviewId,
      @Param("after") Instant after,
      Pageable pageable
  );
}