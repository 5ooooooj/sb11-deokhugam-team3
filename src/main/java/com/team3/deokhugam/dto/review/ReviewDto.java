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
    return new ReviewDto(
        review.getId(),
        review.getBookId(),
        null,
        null,
        review.getUserId(),
        null,
        review.getContent(),
        review.getRating(),
        review.getLikeCount(),
        review.getCommentCount(),
        false,
        review.getCreatedAt(),
        review.getUpdatedAt()
    );
  }
}