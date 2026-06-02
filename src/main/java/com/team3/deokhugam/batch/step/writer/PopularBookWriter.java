package com.team3.deokhugam.batch.step.writer;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PopularBook;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PopularBookWriter {

  private final PopularBookRepository popularBookRepository;

  public ItemWriter<PopularBook> create(Period period) {
      // db 삭제, 순위 계산 로직 -> stepListener, rankingLister로 이동
      // Writer에서는 저장만
      return chunk -> popularBookRepository.saveAll(chunk.getItems());
  }
}
