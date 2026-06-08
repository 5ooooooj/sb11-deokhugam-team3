package com.team3.deokhugam.controller.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PopularReviewDto;
import com.team3.deokhugam.dto.review.ReviewCreateRequest;
import com.team3.deokhugam.dto.review.ReviewDto;
import com.team3.deokhugam.dto.review.ReviewUpdateRequest;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.dashboard.PopularReviewService;
import com.team3.deokhugam.service.review.ReviewLikeService;
import com.team3.deokhugam.service.review.ReviewService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private ReviewService reviewService;

  @MockitoBean
  private ReviewLikeService reviewLikeService;

  @MockitoBean
  private PopularReviewService popularReviewService;

  @Test
  @DisplayName("POST /api/reviews - 리뷰 등록 성공 시 201과 ReviewDto를 반환한다")
  void createReview_returns201() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(bookId, userId, "재밌어요", 5);

    ReviewDto response = new ReviewDto(
        UUID.randomUUID(), bookId, "테스트 도서", "https://img/thumb.jpg", userId, "작성자닉네임",
        "재밌어요", 5, 0, 0, false, Instant.now(), Instant.now());

    given(reviewService.createReview(any(ReviewCreateRequest.class))).willReturn(response);

    mockMvc.perform(post("/api/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.bookId").value(bookId.toString()))
        .andExpect(jsonPath("$.bookTitle").value("테스트 도서"))
        .andExpect(jsonPath("$.bookThumbnailUrl").value("https://img/thumb.jpg"))
        .andExpect(jsonPath("$.userNickname").value("작성자닉네임"))
        .andExpect(jsonPath("$.rating").value(5))
        .andExpect(jsonPath("$.content").value("재밌어요"));
  }

  @Test
  @DisplayName("POST /api/reviews - 필수값 누락 시 400을 반환한다")
  void createReview_invalid_returns400() throws Exception {
    String invalidJson = "{\"rating\": 5}";

    mockMvc.perform(post("/api/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(invalidJson))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("PATCH /api/reviews/{id} - 리뷰 수정 성공 시 200과 ReviewDto를 반환한다")
  void updateReview_returns200() throws Exception {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    ReviewUpdateRequest request = new ReviewUpdateRequest("수정된 내용", 4);

    ReviewDto response = new ReviewDto(
        reviewId, UUID.randomUUID(), null, null, userId, null,
        "수정된 내용", 4, 0, 0, false, Instant.now(), Instant.now());

    given(reviewService.updateReview(eq(reviewId), eq(userId), any(ReviewUpdateRequest.class)))
        .willReturn(response);

    mockMvc.perform(patch("/api/reviews/{reviewId}", reviewId)
            .header("Deokhugam-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("수정된 내용"))
        .andExpect(jsonPath("$.rating").value(4));
  }

  @Test
  @DisplayName("DELETE /api/reviews/{id} - 리뷰 논리 삭제 성공 시 204를 반환한다")
  void deleteReview_returns204() throws Exception {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/reviews/{reviewId}", reviewId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());

    verify(reviewService).deleteReview(reviewId, userId);
  }

  @Test
  @DisplayName("DELETE /api/reviews/{id}/hard - 리뷰 물리 삭제 성공 시 204를 반환한다")
  void hardDeleteReview_returns204() throws Exception {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    mockMvc.perform(delete("/api/reviews/{reviewId}/hard", reviewId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());

    verify(reviewService).hardDeleteReview(reviewId, userId);
  }

  @Test
  @DisplayName("GET /api/reviews/{id} - 리뷰 상세 조회 성공 시 200과 ReviewDto를 반환한다")
  void getReview_returns200() throws Exception {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();

    ReviewDto response = new ReviewDto(
        reviewId, bookId, "테스트 도서", "https://img/thumb.jpg", userId, "작성자닉네임",
        "재밌어요", 5, 0, 0, false, Instant.now(), Instant.now());

    given(reviewService.getReview(eq(reviewId), eq(userId))).willReturn(response);

    mockMvc.perform(get("/api/reviews/{reviewId}", reviewId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(reviewId.toString()))
        .andExpect(jsonPath("$.bookId").value(bookId.toString()))
        .andExpect(jsonPath("$.bookTitle").value("테스트 도서"))
        .andExpect(jsonPath("$.bookThumbnailUrl").value("https://img/thumb.jpg"))
        .andExpect(jsonPath("$.userNickname").value("작성자닉네임"))
        .andExpect(jsonPath("$.content").value("재밌어요"))
        .andExpect(jsonPath("$.rating").value(5));
  }

  @Test
  @DisplayName("GET /api/reviews/{id} - 존재하지 않는 리뷰면 404를 반환한다")
  void getReview_notFound_returns404() throws Exception {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();

    given(reviewService.getReview(eq(reviewId), eq(userId)))
        .willThrow(new DeokhugamException(ErrorCode.REVIEW_NOT_FOUND));

    mockMvc.perform(get("/api/reviews/{reviewId}", reviewId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isNotFound());
  }

  @Test
  @DisplayName("GET /api/reviews - 리뷰 목록 조회 성공 시 200과 CursorPageResponse를 반환한다")
  void searchReviews_returns200() throws Exception {
    UUID requestUserId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();

    ReviewDto reviewDto = new ReviewDto(
        reviewId, bookId, "테스트 도서", "https://img/thumb.jpg", requestUserId, "작성자닉네임",
        "재밌어요", 5, 0, 0, false, Instant.now(), Instant.now()
    );
    CursorPageResponse<ReviewDto> response = new CursorPageResponse<>(
        List.of(reviewDto), null, null, 1, 1L, false
    );

    given(reviewService.searchReviews(any())).willReturn(response);

    mockMvc.perform(get("/api/reviews")
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content[0].id").value(reviewId.toString()))
        .andExpect(jsonPath("$.content[0].bookTitle").value("테스트 도서"))
        .andExpect(jsonPath("$.content[0].userNickname").value("작성자닉네임"))
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.hasNext").value(false));
  }

  @Test
  @DisplayName("GET /api/reviews - 필수 헤더 누락 시 400을 반환한다 (수정 5)")
  void searchReviews_missingHeader_returns400() throws Exception {
    mockMvc.perform(get("/api/reviews"))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /api/reviews - 잘못된 orderBy 값이면 400을 반환한다 (수정 3)")
  void searchReviews_invalidOrderBy_returns400() throws Exception {
    UUID userId = UUID.randomUUID();
    mockMvc.perform(get("/api/reviews")
            .param("orderBy", "garbage")
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("GET /api/reviews - 잘못된 direction 값이면 400을 반환한다 (수정 3)")
  void searchReviews_invalidDirection_returns400() throws Exception {
    UUID userId = UUID.randomUUID();
    mockMvc.perform(get("/api/reviews")
            .param("direction", "WRONG_VALUE")
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("성공: GET /api/reviews/popular - 200 정상 응답")
  void getPopularReviews_success() throws Exception {
    List<PopularReviewDto> content = List.of(
        new PopularReviewDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "도서1", null, UUID.randomUUID(), "유저1", "내용1", 5,
            Period.DAILY, Instant.now(), 1, BigDecimal.valueOf(90), 10, 5),
        new PopularReviewDto(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
            "도서2", null, UUID.randomUUID(), "유저2", "내용2", 4,
            Period.DAILY, Instant.now(), 2, BigDecimal.valueOf(80), 8, 3)
    );

    CursorPageResponse<PopularReviewDto> mockResponse = new CursorPageResponse<>(
        content, null, null, content.size(), 2L, false
    );

    given(popularReviewService.getPopularReviews(eq("DAILY"), eq(20)))
        .willReturn(mockResponse);

    mockMvc.perform(get("/api/reviews/popular")
            .param("period", "DAILY")
            .param("limit", "20"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].rank").value(1))
        .andExpect(jsonPath("$.hasNext").value(false));

    verify(popularReviewService).getPopularReviews(eq("DAILY"), eq(20));
  }

  @Test
  @DisplayName("실패: GET /api/reviews/popular - 잘못된 period → 400")
  void getPopularReviews_invalidPeriod_returns400() throws Exception {
    given(popularReviewService.getPopularReviews(eq("INVALID"), anyInt()))
        .willThrow(new DeokhugamException(ErrorCode.INVALID_PERIOD));

    mockMvc.perform(get("/api/reviews/popular")
            .param("period", "INVALID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
  }

  @Test
  @DisplayName("실패: GET /api/reviews/popular - limit 0 → 400")
  void getPopularReviews_invalidLimit_returns400() throws Exception {
    given(popularReviewService.getPopularReviews(anyString(), eq(0)))
        .willThrow(new DeokhugamException(ErrorCode.INVALID_INPUT));

    mockMvc.perform(get("/api/reviews/popular")
            .param("limit", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }
}