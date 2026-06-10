package com.team3.deokhugam.dto.dashboard;

import com.team3.deokhugam.batch.global.Period;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PopularReviewDto(
    UUID id,
    UUID reviewId,
    UUID bookId,
    String bookTitle,
    String bookThumbnailUrl,
    UUID userId,
    String userNickname,
    String reviewContent,
    int reviewRating,
    Period period,
    Instant createdAt,
    int rank,
    BigDecimal score,
    int likeCount,
    int commentCount
) {
}
