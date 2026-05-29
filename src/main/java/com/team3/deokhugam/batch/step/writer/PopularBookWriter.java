package com.team3.deokhugam.batch.step.writer;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.book.PopularBook;
import com.team3.deokhugam.repository.book.PopularBookRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.Chunk;
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

      for (int i = 0; i < ranked.size(); i++) {
        ranked.get(i).assignRank(i + 1);
      }

      popularBookRepository.saveAll(ranked);
    };
  }
}
