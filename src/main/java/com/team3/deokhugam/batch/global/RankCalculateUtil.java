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
    if (items == null || items.isEmpty()) {
      return;
    }
    int rank = 1;
    BigDecimal prevScore = null;
    for (int i = 0; i < items.size(); i++) {
      BigDecimal currentScore = scoreExtractor.apply(items.get(i));
      if (currentScore == null) {
        throw new IllegalArgumentException("점수는 비어있을 수 없습니다.");
      }
      if (i > 0 && currentScore.compareTo(prevScore) == 0) {
        rankSetter.accept(items.get(i), rank - 1);
      } else {
        rankSetter.accept(items.get(i), rank);
      }
      prevScore = currentScore;
      rank++;
    }

  }

}
