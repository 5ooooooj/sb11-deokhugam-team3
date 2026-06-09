package com.team3.deokhugam.repository.review;

import com.team3.deokhugam.domain.review.Review;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewRepository extends JpaRepository<Review, UUID>, ReviewRepositoryCustom {

  boolean existsByUser_IdAndBook_Id(UUID userId, UUID bookId);

  Optional<Review> findByIdAndDeletedAtIsNull(UUID id);

  @Modifying
  @Query("UPDATE Review r SET r.likeCount = r.likeCount + 1 WHERE r.id = :reviewId")
  void incrementLikeCount(@Param("reviewId") UUID reviewId);

  @Modifying
  @Query("UPDATE Review r SET r.likeCount = r.likeCount - 1 WHERE r.id = :reviewId AND r.likeCount > 0")
  void decrementLikeCount(@Param("reviewId") UUID reviewId);
}