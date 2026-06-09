package com.team3.deokhugam.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PowerUserDto;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.dashboard.PowerUserRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class PowerUserServiceImplTest {

  @InjectMocks
  private PowerUserServiceImpl powerUserService;

  @Mock
  private PowerUserRepository powerUserRepository;

  @Test
  @DisplayName("성공: 파워 유저 목록을 rank 오름차순으로 반환한다")
  void getPowerUsers_success() {
    List<PowerUserDto> mockContent = List.of(
        new PowerUserDto(UUID.randomUUID(), "유저1", Period.DAILY, Instant.now(), 1,
            BigDecimal.valueOf(90), BigDecimal.valueOf(50), 10, 5),
        new PowerUserDto(UUID.randomUUID(), "유저2", Period.DAILY, Instant.now(), 2,
            BigDecimal.valueOf(80), BigDecimal.valueOf(40), 8, 3)
    );
    given(powerUserRepository.findPowerUsersByPeriod(eq(Period.DAILY), any(Pageable.class)))
        .willReturn(mockContent);
    given(powerUserRepository.countByPeriod(Period.DAILY)).willReturn(2);

    CursorPageResponse<PowerUserDto> result =
        powerUserService.getPowerUsers("DAILY", 10);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content().get(0).rank()).isEqualTo(1);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
  }

  @Test
  @DisplayName("성공: 소문자 period도 정상 처리된다")
  void getPowerUsers_lowercasePeriod_success() {
    given(powerUserRepository.findPowerUsersByPeriod(eq(Period.WEEKLY), any(Pageable.class)))
        .willReturn(List.of());
    given(powerUserRepository.countByPeriod(Period.WEEKLY)).willReturn(0);

    assertThatNoException().isThrownBy(() ->
        powerUserService.getPowerUsers("weekly", 10)
    );
  }

  @Test
  @DisplayName("실패: 올바르지 않은 period 파라미터면 DeokhugamException 발생")
  void getPowerUsers_invalidPeriod_throwsException() {
    assertThatThrownBy(() ->
        powerUserService.getPowerUsers("INVALID", 10)
    )
        .isInstanceOf(DeokhugamException.class)
        .satisfies(e -> assertThat(((DeokhugamException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_PERIOD));
  }

  @Test
  @DisplayName("실패: limit이 0 이하면 DeokhugamException 발생")
  void getPowerUsers_invalidLimit_throwsException() {
    assertThatThrownBy(() ->
        powerUserService.getPowerUsers("DAILY", 0)
    )
        .isInstanceOf(DeokhugamException.class)
        .satisfies(e -> assertThat(((DeokhugamException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_INPUT));
  }
}