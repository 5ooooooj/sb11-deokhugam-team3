package com.team3.deokhugam.batch.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PopularReviewRawData(
    UUID reviewId,
    int likeCount,
    int commentCount,
    BigDecimal score
) {
}
