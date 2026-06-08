package com.team3.deokhugam.service.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.domain.book.Book;
import com.team3.deokhugam.domain.review.Review;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.review.ReviewCreateRequest;
import com.team3.deokhugam.dto.review.ReviewDto;
import com.team3.deokhugam.dto.review.ReviewOrderBy;
import com.team3.deokhugam.dto.review.ReviewSearchRequest;
import com.team3.deokhugam.dto.review.ReviewUpdateRequest;
import com.team3.deokhugam.exception.book.BookNotFoundException;
import com.team3.deokhugam.exception.review.ReviewAlreadyExistsException;
import com.team3.deokhugam.exception.review.ReviewForbiddenException;
import com.team3.deokhugam.exception.review.ReviewNotFoundException;
import com.team3.deokhugam.exception.user.UserNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.repository.book.BookRepository;
import com.team3.deokhugam.repository.review.ReviewLikeRepository;
import com.team3.deokhugam.repository.review.ReviewRepository;
import com.team3.deokhugam.repository.user.UserRepository;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

  @Mock
  private ReviewRepository reviewRepository;

  @Mock
  private ReviewLikeRepository reviewLikeRepository;

  @Mock
  private UserRepository userRepository;

  @Mock
  private BookRepository bookRepository;

  @InjectMocks
  private ReviewService reviewService;

  @Test
  @DisplayName("리뷰 등록 성공 - 중복이 없으면 저장하고 ReviewDto를 반환한다")
  void createReview_success() {
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(bookId, userId, "재밌어요", 5);

    User user = mock(User.class);
    Book book = mock(Book.class);
    given(user.getId()).willReturn(userId);
    given(user.getNickname()).willReturn("작성자닉네임");
    given(book.getId()).willReturn(bookId);
    given(book.getTitle()).willReturn("테스트 도서");
    given(book.getThumbnailUrl()).willReturn("https://img/thumb.jpg");
    given(reviewRepository.existsByUser_IdAndBook_Id(userId, bookId)).willReturn(false);
    given(userRepository.findActiveById(userId)).willReturn(Optional.of(user));
    given(bookRepository.findByIdAndDeletedAtIsNull(bookId)).willReturn(Optional.of(book));
    given(reviewRepository.save(any(Review.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    ReviewDto result = reviewService.createReview(request);

    assertThat(result).isNotNull();
    assertThat(result.bookId()).isEqualTo(bookId);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.bookTitle()).isEqualTo("테스트 도서");
    assertThat(result.bookThumbnailUrl()).isEqualTo("https://img/thumb.jpg");
    assertThat(result.userNickname()).isEqualTo("작성자닉네임");
    assertThat(result.rating()).isEqualTo(5);
    assertThat(result.content()).isEqualTo("재밌어요");
    assertThat(result.likedByMe()).isFalse();
    verify(reviewRepository).save(any(Review.class));
  }

  @Test
  @DisplayName("리뷰 등록 실패 - 이미 작성한 리뷰가 있으면 ReviewAlreadyExistsException이 발생한다")
  void createReview_duplicate_throws() {
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(bookId, userId, "또 씀", 4);

    given(reviewRepository.existsByUser_IdAndBook_Id(userId, bookId)).willReturn(true);

    assertThatThrownBy(() -> reviewService.createReview(request))
        .isInstanceOf(ReviewAlreadyExistsException.class);

    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  @DisplayName("리뷰 등록 실패 - 작성자가 없으면 UserNotFoundException이 발생한다")
  void createReview_userNotFound_throws() {
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(bookId, userId, "재밌어요", 5);

    given(reviewRepository.existsByUser_IdAndBook_Id(userId, bookId)).willReturn(false);
    given(userRepository.findActiveById(userId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.createReview(request))
        .isInstanceOf(UserNotFoundException.class);

    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  @DisplayName("리뷰 등록 실패 - 도서가 없으면 BookNotFoundException이 발생한다")
  void createReview_bookNotFound_throws() {
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(bookId, userId, "재밌어요", 5);

    given(reviewRepository.existsByUser_IdAndBook_Id(userId, bookId)).willReturn(false);
    given(userRepository.findActiveById(userId)).willReturn(Optional.of(mock(User.class)));
    given(bookRepository.findByIdAndDeletedAtIsNull(bookId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.createReview(request))
        .isInstanceOf(BookNotFoundException.class);

    verify(reviewRepository, never()).save(any(Review.class));
  }

  @Test
  @DisplayName("리뷰 수정 성공 - 본인 리뷰면 rating·content가 반영된다")
  void updateReview_success() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = reviewWith(userId, UUID.randomUUID(), 3, "예전 내용");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(reviewLikeRepository.existsByReview_IdAndUser_Id(reviewId, userId))
        .willReturn(false);

    ReviewDto result = reviewService.updateReview(
        reviewId, userId, new ReviewUpdateRequest("새 내용", 5));

    assertThat(result.rating()).isEqualTo(5);
    assertThat(result.content()).isEqualTo("새 내용");
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 본인이 아니면 ReviewForbiddenException이 발생한다")
  void updateReview_notOwner_throws() {
    UUID reviewId = UUID.randomUUID();
    UUID ownerId = UUID.randomUUID();
    UUID otherUserId = UUID.randomUUID();
    Review review = reviewWith(ownerId, UUID.randomUUID(), 3, "내용");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    assertThatThrownBy(() -> reviewService.updateReview(
        reviewId, otherUserId, new ReviewUpdateRequest("수정", 4)))
        .isInstanceOf(ReviewForbiddenException.class);
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 리뷰가 없으면 ReviewNotFoundException이 발생한다")
  void updateReview_notFound_throws() {
    UUID reviewId = UUID.randomUUID();
    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.updateReview(
        reviewId, UUID.randomUUID(), new ReviewUpdateRequest("수정", 4)))
        .isInstanceOf(ReviewNotFoundException.class);
  }

  @Test
  @DisplayName("리뷰 논리 삭제 성공 - 본인 리뷰면 삭제 처리된다")
  void deleteReview_success() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = reviewWith(userId, UUID.randomUUID(), 3, "내용");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    reviewService.deleteReview(reviewId, userId);

    assertThat(review.isDeleted()).isTrue();
  }

  @Test
  @DisplayName("리뷰 물리 삭제 성공 - 본인 리뷰면 delete가 호출된다")
  void hardDeleteReview_success() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = reviewWith(userId, UUID.randomUUID(), 3, "내용");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    reviewService.hardDeleteReview(reviewId, userId);

    verify(reviewRepository).delete(review);
  }

  @Test
  @DisplayName("리뷰 수정 실패 - 이미 삭제된 리뷰면 ReviewNotFoundException이 발생한다")
  void updateReview_deleted_throws() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = reviewWith(userId, UUID.randomUUID(), 3, "내용");
    review.softDelete();

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    assertThatThrownBy(() -> reviewService.updateReview(
        reviewId, userId, new ReviewUpdateRequest("수정", 4)))
        .isInstanceOf(ReviewNotFoundException.class);
  }

  @Test
  @DisplayName("리뷰 상세 조회 성공 - 존재하는 리뷰면 ReviewDto를 반환하고 도서·작성자 정보가 채워진다")
  void getReview_success() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    Review review = reviewWith(userId, bookId, 5, "재밌어요");

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));
    given(reviewLikeRepository.existsByReview_IdAndUser_Id(reviewId, userId))
        .willReturn(true);

    ReviewDto result = reviewService.getReview(reviewId, userId);

    assertThat(result).isNotNull();
    assertThat(result.bookId()).isEqualTo(bookId);
    assertThat(result.userId()).isEqualTo(userId);
    assertThat(result.bookTitle()).isEqualTo("테스트 도서");
    assertThat(result.bookThumbnailUrl()).isEqualTo("https://img/thumb.jpg");
    assertThat(result.userNickname()).isEqualTo("작성자닉네임");
    assertThat(result.rating()).isEqualTo(5);
    assertThat(result.content()).isEqualTo("재밌어요");
    assertThat(result.likedByMe()).isTrue();
  }

  @Test
  @DisplayName("리뷰 상세 조회 실패 - 리뷰가 없으면 ReviewNotFoundException이 발생한다")
  void getReview_notFound_throws() {
    UUID reviewId = UUID.randomUUID();
    given(reviewRepository.findById(reviewId)).willReturn(Optional.empty());

    assertThatThrownBy(() -> reviewService.getReview(reviewId, UUID.randomUUID()))
        .isInstanceOf(ReviewNotFoundException.class);
  }

  @Test
  @DisplayName("리뷰 상세 조회 실패 - 논리 삭제된 리뷰면 ReviewNotFoundException이 발생한다")
  void getReview_deleted_throws() {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Review review = reviewWith(userId, UUID.randomUUID(), 5, "삭제될 리뷰");
    review.softDelete();

    given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

    assertThatThrownBy(() -> reviewService.getReview(reviewId, userId))
        .isInstanceOf(ReviewNotFoundException.class);
  }

  @Test
  @DisplayName("리뷰 목록 조회 성공 - 검색 결과를 CursorPageResponse로 반환한다")
  void searchReviews_success() {
    UUID requestUserId = UUID.randomUUID();
    ReviewSearchRequest request = new ReviewSearchRequest(
        null, null, null,
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, requestUserId
    );

    Review r1 = mockReview(Instant.now(), UUID.randomUUID(), 5);
    Review r2 = mockReview(Instant.now(), UUID.randomUUID(), 4);

    given(reviewRepository.search(any(ReviewSearchRequest.class)))
        .willReturn(List.of(r1, r2));
    given(reviewRepository.count(any(ReviewSearchRequest.class))).willReturn(2L);
    given(reviewLikeRepository.findLikedReviewIds(any(), any()))
        .willReturn(List.of());

    CursorPageResponse<ReviewDto> result = reviewService.searchReviews(request);

    assertThat(result).isNotNull();
    assertThat(result.content()).hasSize(2);
    assertThat(result.totalElements()).isEqualTo(2);
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
  }

  @Test
  @DisplayName("리뷰 목록 조회 - limit보다 많이 조회되면 hasNext=true 이고 nextCursor가 Base64로 생성된다")
  void searchReviews_hasNext_buildsCursor() {
    UUID requestUserId = UUID.randomUUID();
    int limit = 2;
    ReviewSearchRequest request = new ReviewSearchRequest(
        null, null, null,
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, limit, requestUserId
    );

    List<Review> reviews = List.of(
        mockReview(Instant.parse("2025-01-15T10:00:00Z"), UUID.randomUUID(), 5),
        mockReview(Instant.parse("2025-01-15T09:00:00Z"), UUID.randomUUID(), 4),
        mockReview(Instant.parse("2025-01-15T08:00:00Z"), UUID.randomUUID(), 3)
    );

    given(reviewRepository.search(any())).willReturn(reviews);
    given(reviewRepository.count(any(ReviewSearchRequest.class))).willReturn(3L);
    given(reviewLikeRepository.findLikedReviewIds(any(), any()))
        .willReturn(List.of());

    CursorPageResponse<ReviewDto> result = reviewService.searchReviews(request);

    assertThat(result.content()).hasSize(limit);
    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isNotNull();
    assertThatNoException().isThrownBy(
        () -> Base64.getUrlDecoder().decode(result.nextCursor()));
  }

  @Test
  @DisplayName("리뷰 목록 조회 - RATING 정렬에서는 nextCursor가 (rating|createdAt|id) 3-part 토큰")
  void searchReviews_ratingOrder_buildsThreePartCursor() {
    UUID requestUserId = UUID.randomUUID();
    ReviewSearchRequest request = new ReviewSearchRequest(
        null, null, null,
        ReviewOrderBy.RATING, Sort.Direction.DESC,
        null, 1, requestUserId
    );

    Review r1 = mockReview(Instant.now(), UUID.randomUUID(), 5);
    Review r2 = mockReview(Instant.now(), UUID.randomUUID(), 4);

    given(reviewRepository.search(any())).willReturn(List.of(r1, r2));
    given(reviewRepository.count(any(ReviewSearchRequest.class))).willReturn(2L);
    given(reviewLikeRepository.findLikedReviewIds(any(), any()))
        .willReturn(List.of());

    CursorPageResponse<ReviewDto> result = reviewService.searchReviews(request);

    assertThat(result.hasNext()).isTrue();
    assertThat(result.nextCursor()).isNotNull();
    String decoded = new String(
        Base64.getUrlDecoder().decode(result.nextCursor()),
        StandardCharsets.UTF_8);
    assertThat(decoded.split("\\|")).hasSize(3);
  }

  @Test
  @DisplayName("리뷰 목록 조회 - 커서가 있으면(첫 페이지가 아니면) count는 호출되지 않는다")
  void searchReviews_withCursor_skipsCount() {
    UUID requestUserId = UUID.randomUUID();
    ReviewSearchRequest request = new ReviewSearchRequest(
        null, null, null,
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        "someCursor", 10, requestUserId
    );

    Review r1 = mockReview(Instant.now(), UUID.randomUUID(), 5);

    given(reviewRepository.search(any())).willReturn(List.of(r1));
    given(reviewLikeRepository.findLikedReviewIds(any(), any()))
        .willReturn(List.of());

    CursorPageResponse<ReviewDto> result = reviewService.searchReviews(request);

    assertThat(result.totalElements()).isEqualTo(0L);
    verify(reviewRepository, never()).count(any(ReviewSearchRequest.class));
  }

  @Test
  @DisplayName("리뷰 목록 조회 - 결과가 비어 있으면 nextCursor=null, hasNext=false")
  void searchReviews_empty() {
    UUID requestUserId = UUID.randomUUID();
    ReviewSearchRequest request = new ReviewSearchRequest(
        null, null, null,
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, requestUserId
    );

    given(reviewRepository.search(any())).willReturn(List.of());
    given(reviewRepository.count(any(ReviewSearchRequest.class))).willReturn(0L);

    CursorPageResponse<ReviewDto> result = reviewService.searchReviews(request);

    assertThat(result.content()).isEmpty();
    assertThat(result.hasNext()).isFalse();
    assertThat(result.nextCursor()).isNull();
    assertThat(result.totalElements()).isEqualTo(0L);
  }

  @Test
  @DisplayName("리뷰 목록 조회 - 내가 좋아요한 리뷰만 likedByMe=true 로 표시된다")
  void searchReviews_populatesLikedByMe() {
    UUID requestUserId = UUID.randomUUID();
    ReviewSearchRequest request = new ReviewSearchRequest(
        null, null, null,
        ReviewOrderBy.CREATED_AT, Sort.Direction.DESC,
        null, 10, requestUserId
    );

    UUID likedId = UUID.randomUUID();
    UUID notLikedId = UUID.randomUUID();
    Review liked = mockReview(Instant.now(), likedId, 5);
    Review notLiked = mockReview(Instant.now(), notLikedId, 4);

    given(reviewRepository.search(any())).willReturn(List.of(liked, notLiked));
    given(reviewRepository.count(any(ReviewSearchRequest.class))).willReturn(2L);
    given(reviewLikeRepository.findLikedReviewIds(any(), any()))
        .willReturn(List.of(likedId));

    CursorPageResponse<ReviewDto> result = reviewService.searchReviews(request);

    ReviewDto likedDto = result.content().stream()
        .filter(d -> d.id().equals(likedId)).findFirst().orElseThrow();
    ReviewDto notLikedDto = result.content().stream()
        .filter(d -> d.id().equals(notLikedId)).findFirst().orElseThrow();

    assertThat(likedDto.likedByMe()).isTrue();
    assertThat(notLikedDto.likedByMe()).isFalse();
  }

  private Review reviewWith(UUID userId, UUID bookId, int rating, String content) {
    User user = mock(User.class);
    Book book = mock(Book.class);
    lenient().when(user.getId()).thenReturn(userId);
    lenient().when(user.getNickname()).thenReturn("작성자닉네임");
    lenient().when(book.getId()).thenReturn(bookId);
    lenient().when(book.getTitle()).thenReturn("테스트 도서");
    lenient().when(book.getThumbnailUrl()).thenReturn("https://img/thumb.jpg");
    return Review.create(user, book, rating, content);
  }

  private Review mockReview(Instant createdAt, UUID id, int rating) {
    Review review = mock(Review.class);
    lenient().when(review.getCreatedAt()).thenReturn(createdAt);
    lenient().when(review.getId()).thenReturn(id);
    lenient().when(review.getRating()).thenReturn(rating);
    lenient().when(review.getUser()).thenReturn(mock(User.class));
    lenient().when(review.getBook()).thenReturn(mock(Book.class));
    return review;
  }
}