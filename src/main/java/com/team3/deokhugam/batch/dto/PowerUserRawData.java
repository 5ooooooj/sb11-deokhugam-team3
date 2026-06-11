package com.team3.deokhugam.batch.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record PowerUserRawData (
    UUID userId,
    BigDecimal reviewScoreSum,
    int likeCount,
    int commentCount,
    BigDecimal score
){
}
