package com.team3.deokhugam.controller.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.deokhugam.dto.review.ReviewLikeDto;
import com.team3.deokhugam.exception.review.ReviewNotFoundException;
import com.team3.deokhugam.service.review.ReviewLikeService;
import com.team3.deokhugam.service.review.ReviewService;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
class ReviewControllerLikeTest {

  @Autowired
  MockMvc mockMvc;

  @MockitoBean
  ReviewService reviewService;

  @MockitoBean
  ReviewLikeService reviewLikeService;

  private static final String HEADER = "Deokhugam-Request-User-ID";

  @Test
  @DisplayName("POST /api/reviews/{reviewId}/like - 좋아요 성공 시 200과 {reviewId, userId, liked} 반환")
  void likeReview_success() throws Exception {
    UUID reviewId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    when(reviewLikeService.toggleLike(any(), any()))
        .thenReturn(new ReviewLikeDto(reviewId, userId, true));

    mockMvc.perform(post("/api/reviews/{reviewId}/like", reviewId)
            .header(HEADER, userId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reviewId").value(reviewId.toString()))
        .andExpect(jsonPath("$.userId").value(userId.toString()))
        .andExpect(jsonPath("$.liked").value(true));
  }

  @Test
  @DisplayName("요청자 ID 헤더 누락 시 400 반환")
  void likeReview_missingHeader() throws Exception {
    mockMvc.perform(post("/api/reviews/{reviewId}/like", UUID.randomUUID()))
        .andExpect(status().isBadRequest());
  }

  @Test
  @DisplayName("존재하지 않는 리뷰면 404 반환")
  void likeReview_reviewNotFound() throws Exception {
    when(reviewLikeService.toggleLike(any(), any()))
        .thenThrow(new ReviewNotFoundException());

    mockMvc.perform(post("/api/reviews/{reviewId}/like", UUID.randomUUID())
            .header(HEADER, UUID.randomUUID()))
        .andExpect(status().isNotFound());
  }
}