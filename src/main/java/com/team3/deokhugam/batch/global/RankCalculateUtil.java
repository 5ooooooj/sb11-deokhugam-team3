package com.team3.deokhugam.batch.global;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RankCalculateUtil {

  private RankCalculateUtil() {}

  public static <T> void assignRanks(
      List<T> items,
      Function<T, BigDecimal> scoreExtractor,
      BiConsumer<T, Integer> rankSetter)
  {
    int rank = 1;
    for (int i = 0; i < items.size(); i++) {
      if (i > 0 && scoreExtractor.apply(items.get(i))
          .compareTo(scoreExtractor.apply(items.get(i - 1))) == 0) {
        rankSetter.accept(items.get(i), rank - 1);
      } else {
        rankSetter.accept(items.get(i), rank);
      }
      rank++;
    }

  }

}
