package com.team3.deokhugam.batch.global;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class DateCalculateUtil {

  public static Instant getStartDate(Period period) {
    LocalDate start = switch (period) {
      case DAILY -> LocalDate.now();
      case WEEKLY -> LocalDate.now().minusWeeks(1);
      case MONTHLY -> LocalDate.now().minusMonths(1);
      case ALL_TIME -> null;
    };

    if (start == null) return null;
    return start.atStartOfDay(ZoneId.systemDefault()).toInstant();
  }
}
