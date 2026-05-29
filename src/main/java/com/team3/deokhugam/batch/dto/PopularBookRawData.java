package com.team3.deokhugam.batch.dto;

import java.util.UUID;

public record PopularBookRawData (
    UUID bookId,
    int reviewCount,
    double ratingAvg
){}
