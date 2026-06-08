package com.team3.deokhugam.service.dashboard;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PowerUserDto;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.dashboard.PowerUserRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PowerUserServiceImpl implements PowerUserService {

  private final PowerUserRepository powerUserRepository;

  @Override
  public CursorPageResponse<PowerUserDto> getPowerUsers(String period, int limit) {
    validateLimit(limit);
    Period parsedPeriod = parsePeriod(period);

    List<PowerUserDto> content = powerUserRepository
        .findPowerUsersByPeriod(parsedPeriod, PageRequest.of(0, limit));

    return new CursorPageResponse<>(
        content,
        null,
        null,
        content.size(),
        powerUserRepository.countByPeriod(parsedPeriod),
        false
    );
  }

  private Period parsePeriod(String period) {
    try {
      return Period.valueOf(period.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("올바르지 않은 period 파라미터: {}", period);
      throw new DeokhugamException(ErrorCode.INVALID_PERIOD);
    }
  }

  private void validateLimit(int limit) {
    if (limit < 1) {
      throw new DeokhugamException(ErrorCode.INVALID_INPUT);
    }
  }
}
