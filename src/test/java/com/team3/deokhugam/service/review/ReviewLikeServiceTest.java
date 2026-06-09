package com.team3.deokhugam.service.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.review.ReviewLike;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.review.ReviewLikeDto;
import com.team3.deokhugam.exception.review.ReviewNotFoundException;
import com.team3.deokhugam.repository.review.ReviewLikeRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import com.team3.deokhugam.service.notification.NotificationService;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewLikeServiceTest {

  @Mock ReviewLikeRepository reviewLikeRepository;
  @Mock ReviewRepository reviewRepository;
  @Mock UserRepository userRepository;
  @Mock NotificationService notificationService;

  @InjectMocks ReviewLikeService reviewLikeService;

  private UUID reviewId;
  private UUID userId;
  private Review review;

  @BeforeEach
  void setUp() {
    reviewId = UUID.randomUUID();
    userId = UUID.randomUUID();
    review = Review.create(mock(User.class), mock(Book.class), 5, "내용");
    ReflectionTestUtils.setField(review, "id", reviewId);
  }

  @Test
  @DisplayName("좋아요가 없던 리뷰에 누르면 추가되고 like_count가 +1, liked=true")
  void toggleLike_add() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewLikeRepository.findByReview_IdAndUser_Id(reviewId, userId))
        .thenReturn(Optional.empty());
    when(userRepository.getReferenceById(userId))
        .thenReturn(new User("a@b.com", "닉네임", "pw"));

    ReviewLikeDto result = reviewLikeService.toggleLike(reviewId, userId);

    assertThat(result.liked()).isTrue();
    assertThat(result.reviewId()).isEqualTo(reviewId);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(review.getLikeCount()).isEqualTo(1);
    verify(reviewLikeRepository).saveAndFlush(any(ReviewLike.class));
  }

  @Test
  @DisplayName("이미 좋아요한 리뷰에 다시 누르면 취소되고 like_count가 -1, liked=false")
  void toggleLike_cancel() {
    review.increaseLikeCount();
    ReviewLike existing = ReviewLike.create(review, new User("a@b.com", "닉", "pw"));
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewLikeRepository.findByReview_IdAndUser_Id(reviewId, userId))
        .thenReturn(Optional.of(existing));

    ReviewLikeDto result = reviewLikeService.toggleLike(reviewId, userId);

    assertThat(result.liked()).isFalse();
    assertThat(review.getLikeCount()).isZero();
    verify(reviewLikeRepository).delete(existing);
    verify(notificationService, never()).createLikeNotification(any(), any());
  }

  @Test
  @DisplayName("존재하지 않는 리뷰 ID로 좋아요 시 ReviewNotFoundException(404) 발생")
  void toggleLike_reviewNotFound() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> reviewLikeService.toggleLike(reviewId, userId))
        .isInstanceOf(ReviewNotFoundException.class);
  }

  @Test
  @DisplayName("논리 삭제된 리뷰에 좋아요 시 ReviewNotFoundException(404) 발생")
  void toggleLike_deletedReview() {
    review.softDelete();
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));

    assertThatThrownBy(() -> reviewLikeService.toggleLike(reviewId, userId))
        .isInstanceOf(ReviewNotFoundException.class);
  }

  @Test
  @DisplayName("좋아요 추가 시 LIKE 타입 알림 트리거(createLikeNotification) 호출")
  void toggleLike_triggersNotification() {
    when(reviewRepository.findById(reviewId)).thenReturn(Optional.of(review));
    when(reviewLikeRepository.findByReview_IdAndUser_Id(reviewId, userId))
        .thenReturn(Optional.empty());
    when(userRepository.getReferenceById(userId))
        .thenReturn(new User("a@b.com", "닉", "pw"));

    reviewLikeService.toggleLike(reviewId, userId);

    verify(notificationService, times(1)).createLikeNotification(reviewId, userId);
  }
}