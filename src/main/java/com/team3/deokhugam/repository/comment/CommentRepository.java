package com.team3.deokhugam.repository.comment;

import com.team3.deokhugam.domain.comment.Comment;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CommentRepository extends JpaRepository<Comment, UUID> {

  default List<Comment> findByReviewIdWithCursor(UUID reviewId, Instant after, Pageable pageable) {
    if (after == null) {
      return findByReviewIdWithoutCursor(reviewId, pageable);
    }
    return findByReviewIdWithAfter(reviewId, after, pageable);
  }

  @Query("""
      SELECT c
      FROM Comment c
      WHERE c.review.id = :reviewId
        AND c.deletedAt IS NULL
      ORDER BY c.createdAt DESC
      """)
  List<Comment> findByReviewIdWithoutCursor(
      @Param("reviewId") UUID reviewId,
      Pageable pageable
  );

  @Query("""
      SELECT c
      FROM Comment c
      WHERE c.review.id = :reviewId
        AND c.deletedAt IS NULL
        AND c.createdAt < :after
      ORDER BY c.createdAt DESC
      """)
  List<Comment> findByReviewIdWithAfter(
      @Param("reviewId") UUID reviewId,
      @Param("after") Instant after,
      Pageable pageable
  );
}
