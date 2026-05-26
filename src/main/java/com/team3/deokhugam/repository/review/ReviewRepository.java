package com.team3.deokhugam.repository.review;

import com.team3.deokhugam.domain.review.Review;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

  boolean existsByUserIdAndBookId(UUID userId, UUID bookId);
}