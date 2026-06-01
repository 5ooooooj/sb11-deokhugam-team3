package com.team3.deokhugam.batch.global;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class DateCalculateUtil {

  public static Instant getStartDate(Period period) {
    return switch (period) {
      case DAILY -> Instant.now().truncatedTo(ChronoUnit.DAYS);
      case WEEKLY -> Instant.now().minus(7, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      case MONTHLY -> Instant.now().minus(30, ChronoUnit.DAYS).truncatedTo(ChronoUnit.DAYS);
      case ALL_TIME -> null;
    };
  }
}
