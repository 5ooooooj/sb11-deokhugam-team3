package com.team3.deokhugam.service.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PopularReviewDto;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.dashboard.PopularReviewRepository;
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
class PopularReviewServiceImplTest {

  @InjectMocks
  private PopularReviewServiceImpl popularReviewService;

  @Mock
  private PopularReviewRepository popularReviewRepository;

  @Test
  @DisplayName("성공: 인기 리뷰 목록을 rank 오름차순으로 반환한다")
  void getPopularReviews_success() {
    List<PopularReviewDto> mockContent = List.of(
        new PopularReviewDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "도서1", null, UUID.randomUUID(), "유저1", "내용1", 5,
            Period.DAILY, Instant.now(), 1, BigDecimal.valueOf(90), 10, 5),
        new PopularReviewDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "도서2", null, UUID.randomUUID(), "유저2", "내용2", 4,
            Period.DAILY, Instant.now(), 2, BigDecimal.valueOf(80), 8, 3)
    );
    given(popularReviewRepository.findPopularReviewsByPeriod(eq(Period.DAILY), any(Pageable.class)))
        .willReturn(mockContent);
    given(popularReviewRepository.countByPeriod(Period.DAILY)).willReturn(2);

    CursorPageResponse<PopularReviewDto> result =
        popularReviewService.getPopularReviews("DAILY", 20);

    assertThat(result.content()).hasSize(2);
    assertThat(result.content().get(0).rank()).isEqualTo(1);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
  }

  @Test
  @DisplayName("성공: 소문자 period도 정상 처리된다")
  void getPopularReviews_lowercasePeriod_success() {
    given(popularReviewRepository.findPopularReviewsByPeriod(eq(Period.WEEKLY), any(Pageable.class)))
        .willReturn(List.of());
    given(popularReviewRepository.countByPeriod(Period.WEEKLY)).willReturn(0);

    assertThatNoException().isThrownBy(() ->
        popularReviewService.getPopularReviews("weekly", 20)
    );
  }

  @Test
  @DisplayName("실패: 올바르지 않은 period 파라미터면 DeokhugamException 발생")
  void getPopularReviews_invalidPeriod_throwsException() {
    assertThatThrownBy(() ->
        popularReviewService.getPopularReviews("INVALID", 20)
    )
        .isInstanceOf(DeokhugamException.class)
        .satisfies(e -> assertThat(((DeokhugamException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_PERIOD));
  }

  @Test
  @DisplayName("실패: limit이 0 이하면 DeokhugamException 발생")
  void getPopularBooks_invalidLimit_throwsException() {
    assertThatThrownBy(() ->
        popularReviewService.getPopularReviews("DAILY", 0)
    )
        .isInstanceOf(DeokhugamException.class)
        .satisfies(e -> assertThat(((DeokhugamException) e).getErrorCode())
            .isEqualTo(ErrorCode.INVALID_INPUT));
  }
}