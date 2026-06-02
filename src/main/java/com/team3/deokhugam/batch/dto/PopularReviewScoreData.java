package com.team3.deokhugam.batch.dto;

import java.util.UUID;

public record PopularReviewScoreData (
    UUID reviewId,
    int likeCount,
    int commentCount) {
}
