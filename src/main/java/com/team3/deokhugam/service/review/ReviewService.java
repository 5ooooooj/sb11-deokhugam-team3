package com.team3.deokhugam.service.review;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.dto.review.ReviewCreateRequest;
import com.team3.deokhugam.dto.review.ReviewDto;
import com.team3.deokhugam.repository.review.ReviewRepository;
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
      throw new IllegalStateException("이미 해당 도서에 작성한 리뷰가 있습니다.");
    }

    Review review = Review.create(
        request.userId(),
        request.bookId(),
        request.rating(),
        request.content());

    Review saved = reviewRepository.save(review);
    return ReviewDto.from(saved);
  }
}