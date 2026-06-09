package com.team3.deokhugam.batch.global;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

public class DateCalculateUtil {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");

  public static Instant getStartDate(Period period) {
    ZonedDateTime nowKst = ZonedDateTime.now(KST);
    return switch (period) {
      case DAILY -> nowKst.toLocalDate().atStartOfDay(KST).toInstant();
      case WEEKLY -> nowKst.toLocalDate().minusDays(7).atStartOfDay(KST).toInstant();
      case MONTHLY -> nowKst.toLocalDate().minusDays(30).atStartOfDay(KST).toInstant();
      case ALL_TIME -> null;
    };
  }
}
