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
import com.team3.deokhugam.dto.review.ReviewUpdateRequest;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.repository.review.ReviewRepository;
import java.util.Optional;
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
        .isInstanceOf(DeokhugamException.class);

    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  @DisplayName("리뷰 수정 성공 - 본인 리뷰면 rating·content가 반영된다")
  void updateReview_success() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = Review.create(userId, UUID.randomUUID(), 3, "예전 내용");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    ReviewDto result = reviewService.updateReview(
        reviewId, userId, new ReviewUpdateRequest("새 내용", 5));

    assertThat(result.rating()).isEqualTo(5);
    assertThat(result.content()).isEqualTo("새 내용");
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 본인이 아니면 예외가 발생한다")
  void updateReview_notOwner_throws() {
    UUID reviewId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    Review review = Review.create(ownerId, UUID.randomUUID(), 3, "내용");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    assertThatThrownBy(() -> reviewService.updateReview(
        reviewId, otherUserId, new ReviewUpdateRequest("수정", 4)))
        .isInstanceOf(DeokhugamException.class);
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 리뷰가 없으면 예외가 발생한다")
  void updateReview_notFound_throws() {
    UUID reviewId = UUID.randomUUID();
    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.updateReview(
        reviewId, UUID.randomUUID(), new ReviewUpdateRequest("수정", 4)))
        .isInstanceOf(DeokhugamException.class);
  }

  @Test
  @DisplayName("리뷰 논리 삭제 성공 - 본인 리뷰면 삭제 처리된다")
  void deleteReview_success() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = Review.create(userId, UUID.randomUUID(), 3, "내용");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    reviewService.deleteReview(reviewId, userId);

    assertThat(review.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("리뷰 물리 삭제 성공 - 본인 리뷰면 delete가 호출된다")
  void hardDeleteReview_success() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = Review.create(userId, UUID.randomUUID(), 3, "내용");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    reviewService.hardDeleteReview(reviewId, userId);

    verify(reviewRepository).delete(review);
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 이미 삭제된 리뷰면 예외가 발생한다")
  void updateReview_deleted_throws() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = Review.create(userId, UUID.randomUUID(), 3, "내용");
    review.softDelete();  // 미리 삭제 처리

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    assertThatThrownBy(() -> reviewService.updateReview(
        reviewId, userId, new ReviewUpdateRequest("수정", 4)))
        .isInstanceOf(DeokhugamException.class);
  }

  @Test
  @DisplayName("리뷰 상세 조회 성공 - 존재하는 리뷰면 ReviewDto를 반환한다")
  void getReview_success() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    Review review = Review.create(userId, bookId, 5, "재밌어요");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    ReviewDto result = reviewService.getReview(reviewId, userId);

    assertThat(result).isNotNull();
    assertThat(result.bookId()).isEqualTo(bookId);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.rating()).isEqualTo(5);
    assertThat(result.content()).isEqualTo("재밌어요");
  }

  @Test
  @DisplayName("리뷰 상세 조회 실패 - 리뷰가 없으면 예외가 발생한다")
  void getReview_notFound_throws() {
    UUID reviewId = UUID.randomUUID();
    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.getReview(reviewId, UUID.randomUUID()))
        .isInstanceOf(DeokhugamException.class);
  }

  @Test
  @DisplayName("리뷰 상세 조회 실패 - 논리 삭제된 리뷰면 예외가 발생한다")
  void getReview_deleted_throws() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = Review.create(userId, UUID.randomUUID(), 5, "삭제될 리뷰");
    review.softDelete();  // 미리 삭제 처리

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    assertThatThrownBy(() -> reviewService.getReview(reviewId, userId))
        .isInstanceOf(DeokhugamException.class);
  }
}