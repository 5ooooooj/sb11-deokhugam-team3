package com.team3.deokhugam.dto.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PopularBookDto(
    UUID id,
    UUID bookId,
    String title,
    String author,
    String thumbnailUrl,
    Period period,
    int rank,
    BigDecimal score,
    int reviewCount,
    BigDecimal rating,
    Instant createdAt
) {
}
