package com.team3.deokhugam.service.review;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.dto.review.ReviewCreateRequest;
import com.team3.deokhugam.dto.review.ReviewDto;
import com.team3.deokhugam.dto.review.ReviewOrderBy;
import com.team3.deokhugam.dto.review.ReviewSearchRequest;
import com.team3.deokhugam.dto.review.ReviewUpdateRequest;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.review.ReviewRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class ReviewService {

  private static final int PAGE_FETCH_OVER = 1;

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

  @Transactional(readOnly = true)
  public ReviewDto getReview(UUID reviewId, UUID requestUserId) {
    Review review = reviewRepository.findById(reviewId)
        .orElseThrow(() -> new DeokhugamException(ErrorCode.REVIEW_NOT_FOUND));
    if (review.isDeleted()) {
      throw new DeokhugamException(ErrorCode.REVIEW_NOT_FOUND);
    }
    return ReviewDto.from(review);
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<ReviewDto> searchReviews(ReviewSearchRequest request) {
    // hasNext 판별을 위해 limit + 1 만큼 조회한다.
    ReviewSearchRequest pageRequest = request.withLimit(request.limit() + PAGE_FETCH_OVER);
    List<Review> reviews = reviewRepository.search(pageRequest);

    boolean hasNext = reviews.size() > request.limit();
    List<Review> pageReviews = hasNext ? reviews.subList(0, request.limit()) : reviews;

    List<ReviewDto> content = pageReviews.stream()
        .map(ReviewDto::from)
        .toList();


    String nextCursor = hasNext
        ? buildNextCursor(pageReviews.get(pageReviews.size() - 1), request.orderBy())
        : null;


    long totalElements = request.hasCursor() ? 0L : reviewRepository.count(request);

    return new CursorPageResponse<>(
        content,
        nextCursor,
        null,
        content.size(),
        totalElements,
        hasNext
    );
  }


  private String buildNextCursor(Review review, ReviewOrderBy orderBy) {
    String raw = switch (orderBy) {
      case CREATED_AT -> review.getCreatedAt().toString() + "|" + review.getId();
      case RATING -> review.getRating() + "|"
          + review.getCreatedAt() + "|"
          + review.getId();
    };
    return Base64.getUrlEncoder().withoutPadding()
        .encodeToString(raw.getBytes(StandardCharsets.UTF_8));
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