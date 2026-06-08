package com.team3.deokhugam.dto.dashboard;

import com.team3.deokhugam.batch.global.Period;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PowerUserDto(
    UUID userId,
    String nickname,
    Period period,
    Instant createdAt,
    int rank,
    BigDecimal score,
    BigDecimal reviewScoreSum,
    int likeCount,
    int commentCount
) {
}
