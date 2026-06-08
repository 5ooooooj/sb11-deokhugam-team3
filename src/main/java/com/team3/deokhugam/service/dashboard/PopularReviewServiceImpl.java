package com.team3.deokhugam.service.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PopularReviewDto;
import com.team3.deokhugam.exception.dashboard.InvalidPeriodException;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.dashboard.PopularReviewRepository;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PopularReviewServiceImpl implements PopularReviewService {

  private final PopularReviewRepository popularReviewRepository;

  @Override
  public CursorPageResponse<PopularReviewDto> getPopularReviews(String period, int limit) {
    validateLimit(limit);
    Period parsedPeriod = parsePeriod(period);

    List<PopularReviewDto> content = popularReviewRepository
        .findPopularReviewsByPeriod(parsedPeriod, PageRequest.of(0, limit));

    return new CursorPageResponse<>(
        content,
        null,
        null,
        content.size(),
        popularReviewRepository.countByPeriod(parsedPeriod),
        false
    );
  }

  private Period parsePeriod(String period) {
    try {
      return Period.valueOf(period.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException| NullPointerException e) {
      log.debug("올바르지 않은 period 파라미터: {}", period);
      throw new InvalidPeriodException();
    }
  }

  private void validateLimit(int limit) {
    if (limit < 1) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }
}
