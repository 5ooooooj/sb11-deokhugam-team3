package com.team3.deokhugam.repository.review;

import com.team3.deokhugam.domain.review.ReviewLike;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, UUID> {

  Optional<ReviewLike> findByReview_IdAndUser_Id(UUID reviewId, UUID userId);

  boolean existsByReview_IdAndUser_Id(UUID reviewId, UUID userId);

  @Query("select rl.review.id from ReviewLike rl "
      + "where rl.user.id = :userId and rl.review.id in :reviewIds")
  List<UUID> findLikedReviewIds(@Param("userId") UUID userId,
      @Param("reviewIds") Collection<UUID> reviewIds);
}