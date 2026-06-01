package com.team3.deokhugam.batch.step.writer;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularBookWriter {

  private final PopularBookRepository popularBookRepository;

  public ItemWriter<PopularBook> create(Period period) {
    return chunk -> {
      // 기존 데이터 삭제
      popularBookRepository.deleteByPeriod(period);

      List<PopularBook> ranked = new ArrayList<>(chunk.getItems());
      ranked.sort(Comparator.comparingDouble(PopularBook::getScore).reversed());

      int rank = 1;
      for (int i = 0; i < ranked.size(); i++) {
        if (i > 0 && ranked.get(i).getScore() == ranked.get(i - 1).getScore()) {
          ranked.get(i).assignRank(ranked.get(i - 1).getRank());
        } else {
          ranked.get(i).assignRank(i + 1);
        }
        rank++;
      }

      popularBookRepository.saveAll(ranked);
    };
  }
}
