package com.team3.deokhugam.service.review;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.review.ReviewCreateRequest;
import com.team3.deokhugam.dto.review.ReviewDto;
import com.team3.deokhugam.dto.review.ReviewOrderBy;
import com.team3.deokhugam.dto.review.ReviewSearchRequest;
import com.team3.deokhugam.dto.review.ReviewUpdateRequest;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.exception.review.ReviewAlreadyExistsException;
import com.team3.deokhugam.exception.review.ReviewForbiddenException;
import com.team3.deokhugam.exception.review.ReviewNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.repository.review.ReviewLikeRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

  private static final int PAGE_FETCH_OVER = 1;

  private final ReviewRepository reviewRepository;
  private final ReviewLikeRepository reviewLikeRepository;
  private final UserRepository userRepository;
  private final BookRepository bookRepository;

  @Transactional
  public ReviewDto createReview(ReviewCreateRequest request) {
    boolean exists = reviewRepository.existsByUser_IdAndBook_Id(
        request.userId(), request.bookId());
    if (exists) {
      throw new ReviewAlreadyExistsException();
    }

    User user = userRepository.getReferenceById(request.userId());
    Book book = bookRepository.findById(request.bookId())
        .orElseThrow(BookNotFoundException::new);

    Review review = Review.create(user, book, request.rating(), request.content());

    Review saved = reviewRepository.save(review);
    return ReviewDto.from(saved);
  }

  @Transactional
  public ReviewDto updateReview(UUID reviewId, UUID requestUserId,
      ReviewUpdateRequest request) {
    Review review = findOwnedReview(reviewId, requestUserId);
    review.update(request.rating(), request.content());
    boolean likedByMe =
        reviewLikeRepository.existsByReview_IdAndUser_Id(reviewId, requestUserId);
    return ReviewDto.from(review, likedByMe);
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
        .orElseThrow(ReviewNotFoundException::new);
    if (review.isDeleted()) {
      throw new ReviewNotFoundException();
    }
    boolean likedByMe =
        reviewLikeRepository.existsByReview_IdAndUser_Id(reviewId, requestUserId);
    return ReviewDto.from(review, likedByMe);
  }

  @Transactional(readOnly = true)
  public CursorPageResponse<ReviewDto> searchReviews(ReviewSearchRequest request) {
    ReviewSearchRequest pageRequest = request.withLimit(request.limit() + PAGE_FETCH_OVER);
    List<Review> reviews = reviewRepository.search(pageRequest);

    boolean hasNext = reviews.size() > request.limit();
    List<Review> pageReviews = hasNext ? reviews.subList(0, request.limit()) : reviews;

    List<UUID> reviewIds = pageReviews.stream().map(Review::getId).toList();
    Set<UUID> likedIds = reviewIds.isEmpty()
        ? Set.of()
        : new HashSet<>(
            reviewLikeRepository.findLikedReviewIds(request.requestUserId(), reviewIds));

    List<ReviewDto> content = pageReviews.stream()
        .map(r -> ReviewDto.from(r, likedIds.contains(r.getId())))
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
        .orElseThrow(ReviewNotFoundException::new);

    if (review.isDeleted()) {
      throw new ReviewNotFoundException();
    }

    if (!review.getUserId().equals(requestUserId)) {
      throw new ReviewForbiddenException();
    }
    return review;
  }
}