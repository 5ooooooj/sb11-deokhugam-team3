package com.team3.deokhugam.service.review;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.dto.review.ReviewCreateRequest;
import com.team3.deokhugam.dto.review.ReviewDto;
import com.team3.deokhugam.dto.review.ReviewUpdateRequest;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import com.team3.deokhugam.repository.review.ReviewRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private final ReviewRepository reviewRepository;

  @Transactional
  public ReviewDto createReview(ReviewCreateRequest request) {
    boolean exists = reviewRepository.existsByUserIdAndBookId(
        request.userId(), request.bookId());
    if (exists) {
      throw new DeokhugamException(ErrorCode.REVIEW_ALREADY_EXISTS);
    }

    Review review = Review.create(
        request.userId(),
        request.bookId(),
        request.rating(),
        request.content());

    Review saved = reviewRepository.save(review);
    return ReviewDto.from(saved);
  }

  @Transactional
  public ReviewDto updateReview(UUID reviewId, UUID requestUserId,
      ReviewUpdateRequest request) {
    Review review = findOwnedReview(reviewId, requestUserId);
    review.update(request.rating(), request.content());
    return ReviewDto.from(review);
  }

  @Transactional
  public void deleteReview(UUID reviewId, UUID requestUserId) {
    Review review = findOwnedReview(reviewId, requestUserId);
    review.softDelete();
  }

  @Transactional
  public void hardDeleteReview(UUID reviewId, UUID requestUserId) {
    Review review = findOwnedReview(reviewId, requestUserId);
    reviewRepository.delete(review);
  }

  private Review findOwnedReview(UUID reviewId, UUID requestUserId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeokhugamException(ErrorCode.REVIEW_NOT_FOUND));

    if (review.isDeleted()) {
      throw new DeokhugamException(ErrorCode.REVIEW_NOT_FOUND);
    }

    if (!review.getUserId().equals(requestUserId)) {
      throw new DeokhugamException(ErrorCode.REVIEW_FORBIDDEN);
    }
    return review;
  }
}