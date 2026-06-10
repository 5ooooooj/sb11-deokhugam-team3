package com.team3.deokhugam.service.review;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewLike;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.review.ReviewLikeDto;
import com.team3.deokhugam.exception.review.ReviewNotFoundException;
import com.team3.deokhugam.repository.review.ReviewLikeRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import com.team3.deokhugam.service.notification.NotificationService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewLikeService {

  private final ReviewLikeRepository reviewLikeRepository;
  private final ReviewRepository reviewRepository;
  private final UserRepository userRepository;
  private final NotificationService notificationService;

  @Transactional
  public ReviewLikeDto toggleLike(UUID reviewId, UUID userId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(ReviewNotFoundException::new);
    if (review.isDeleted()) {
      throw new ReviewNotFoundException();
    }

    return reviewLikeRepository.findByReview_IdAndUser_Id(reviewId, userId)
        .map(existing -> cancelLike(review, userId, existing))
        .orElseGet(() -> addLike(review, userId));
  }

  private ReviewLikeDto cancelLike(Review review, UUID userId, ReviewLike like) {
    reviewLikeRepository.delete(like);
    reviewRepository.decrementLikeCount(review.getId());
    return new ReviewLikeDto(review.getId(), userId, false);
  }

  private ReviewLikeDto addLike(Review review, UUID userId) {
    try {
      User userRef = userRepository.getReferenceById(userId);
      reviewLikeRepository.saveAndFlush(ReviewLike.create(review, userRef));
      reviewRepository.incrementLikeCount(review.getId());
      notificationService.createLikeNotification(review.getId(), userId);
      return new ReviewLikeDto(review.getId(), userId, true);
    } catch (DataIntegrityViolationException e) {
      return new ReviewLikeDto(review.getId(), userId, true);
    }
  }
}