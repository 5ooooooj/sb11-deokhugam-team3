package com.team3.deokhugam.dto.review;

import com.team3.deokhugam.domain.review.Review;
import java.time.Instant;
import java.util.UUID;

public record ReviewDto(
    UUID id,
    UUID bookId,
    String bookTitle,
    String bookThumbnailUrl,
    UUID userId,
    String userNickname,
    String content,
    int rating,
    int likeCount,
    int commentCount,
    boolean likedByMe,
    Instant createdAt,
    Instant updatedAt
) {

  public static ReviewDto from(Review review) {
    return from(review, false);
  }

  public static ReviewDto from(Review review, boolean likedByMe) {
    return new ReviewDto(
        review.getId(),
        review.getBookId(),
        review.getBook().getTitle(),
        review.getBook().getThumbnailUrl(),
        review.getUserId(),
        review.getUser().getNickname(),
        review.getContent(),
        review.getRating(),
        review.getLikeCount(),
        review.getCommentCount(),
        likedByMe,
        review.getCreatedAt(),
        review.getUpdatedAt()
    );
  }
}