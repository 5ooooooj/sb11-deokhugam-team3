package com.team3.deokhugam.batch.dto;

import com.team3.deokhugam.batch.global.Period;
import java.math.BigDecimal;
import java.util.UUID;

public record PowerUserRawData (
    UUID userId,
    Period period,
    BigDecimal reviewScoreSum,
    int likeCount,
    int commentCount
){
}
