package com.team3.deokhugam.service.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PopularBookDto;
import com.team3.deokhugam.exception.dashboard.InvalidPeriodException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularBookServiceImpl implements PopularBookService{

  private final PopularBookRepository popularBookRepository;

  @Override
  public CursorPageResponse<PopularBookDto> getPopularBooks(String period, int limit) {
    Period parsedPeriod = parsePeriod(period);

    List<PopularBookDto> content = popularBookRepository
        .findPopularBooksByPeriod(parsedPeriod, PageRequest.of(0, limit));

    return new CursorPageResponse<>(
        content,
        null,
        null,
        content.size(),
        popularBookRepository.countByPeriod(parsedPeriod),
        false
    );
  }

  private Period parsePeriod(String period) {
    try {
      return Period.valueOf(period.toUpperCase());
    } catch (IllegalStateException e) {
      log.debug("올바르지 않은 period 파라미터: {}", period);
      throw new InvalidPeriodException();
    }
  }

}
