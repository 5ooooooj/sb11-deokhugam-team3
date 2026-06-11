package com.team3.deokhugam.batch.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PopularBookRawData (
    UUID bookId,
    int reviewCount,
    BigDecimal ratingAvg,
    BigDecimal score
){}
