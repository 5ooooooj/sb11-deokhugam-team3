package com.team3.deokhugam.service.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.dto.review.ReviewCreateRequest;
import com.team3.deokhugam.dto.review.ReviewDto;
import com.team3.deokhugam.repository.review.ReviewRepository;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReviewRepository reviewRepository;

  @InjectMocks
  private ReviewService reviewService;

  @Test
  @DisplayName("리뷰 등록 성공 - 중복이 없으면 저장하고 ReviewDto를 반환한다")
  void createReview_success() {
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(bookId, userId, "재밌어요", 5);

    given(reviewRepository.existsByUserIdAndBookId(userId, bookId)).willReturn(false);
    given(reviewRepository.save(any(Review.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    ReviewDto result = reviewService.createReview(request);

    assertThat(result).isNotNull();
    assertThat(result.bookId()).isEqualTo(bookId);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.rating()).isEqualTo(5);
    assertThat(result.content()).isEqualTo("재밌어요");
    verify(reviewRepository).save(any(Review.class));
  }

  @Test
  @DisplayName("리뷰 등록 실패 - 이미 작성한 리뷰가 있으면 예외가 발생한다")
  void createReview_duplicate_throws() {
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(bookId, userId, "또 씀", 4);

    given(reviewRepository.existsByUserIdAndBookId(userId, bookId)).willReturn(true);

    assertThatThrownBy(() -> reviewService.createReview(request))
        .isInstanceOf(IllegalStateException.class);

    verify(reviewRepository, never()).save(any(Review.class));
  }
}