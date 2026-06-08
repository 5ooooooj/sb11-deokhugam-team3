package com.team3.deokhugam.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PopularBookDto;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.dashboard.PopularBookRepository;
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
class PopularBookServiceImplTest {

  @InjectMocks
  private PopularBookServiceImpl popularBookService;

  @Mock
  private PopularBookRepository popularBookRepository;

  @Test
  @DisplayName("성공: 인기 도서 목록을 rank 오름차순으로 반환한다")
  void getPopularBooks_success() {
    List<PopularBookDto> mockContent = List.of(
        new PopularBookDto(UUID.randomUUID(), UUID.randomUUID(), "도서1", "저자1", null, Period.DAILY, 1, BigDecimal.valueOf(90), 10, BigDecimal.valueOf(4.5), Instant.now()),
        new PopularBookDto(UUID.randomUUID(), UUID.randomUUID(), "도서2", "저자2", null, Period.DAILY, 2, BigDecimal.valueOf(80), 8, BigDecimal.valueOf(4.0), Instant.now())
    );
    given(popularBookRepository.findPopularBooksByPeriod(eq(Period.DAILY), any(Pageable.class)))
        .willReturn(mockContent);
    given(popularBookRepository.countByPeriod(Period.DAILY)).willReturn(2);

    CursorPageResponse<PopularBookDto> result =
        popularBookService.getPopularBooks("DAILY", 50);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content().get(0).rank()).isEqualTo(1);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
  }

  @Test
  @DisplayName("성공: 소문자 period도 정상 처리된다")
  void getPopularBooks_lowercasePeriod_success() {
    given(popularBookRepository.findPopularBooksByPeriod(eq(Period.WEEKLY), any(Pageable.class)))
        .willReturn(List.of());
    given(popularBookRepository.countByPeriod(Period.WEEKLY)).willReturn(0);

    assertThatNoException().isThrownBy(() ->
        popularBookService.getPopularBooks("weekly", 50)
    );
  }

  @Test
  @DisplayName("실패: 올바르지 않은 period 파라미터면 DeokhugamException 발생")
  void getPopularBooks_invalidPeriod_throwsException() {
    assertThatThrownBy(() ->
        popularBookService.getPopularBooks("INVALID", 50)
    )
        .isInstanceOf(DeokhugamException.class)
        .satisfies(e -> assertThat(((DeokhugamException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_PERIOD));
  }

  @Test
  @DisplayName("실패: limit이 0 이하면 DeokhugamException 발생")
  void getPopularBooks_invalidLimit_throwsException() {
    assertThatThrownBy(() ->
        popularBookService.getPopularBooks("DAILY", 0)
    )
        .isInstanceOf(DeokhugamException.class)
        .satisfies(e -> assertThat(((DeokhugamException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_INPUT));
  }
}
