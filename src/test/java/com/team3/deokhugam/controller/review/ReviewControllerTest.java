package com.team3.deokhugam.controller.review;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.dto.review.ReviewCreateRequest;
import com.team3.deokhugam.dto.review.ReviewDto;
import com.team3.deokhugam.service.review.ReviewService;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ReviewController.class)
class ReviewControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockBean
  private ReviewService reviewService;

  @Test
  @DisplayName("POST /api/reviews - 리뷰 등록 성공 시 201과 ReviewDto를 반환한다")
  void createReview_returns201() throws Exception {
    UUID userId = UUID.randomUUID();
    UUID bookId = UUID.randomUUID();
    ReviewCreateRequest request = new ReviewCreateRequest(bookId, userId, "재밌어요", 5);

    ReviewDto response = new ReviewDto(
        UUID.randomUUID(), bookId, null, null, userId, null,
        "재밌어요", 5, 0, 0, false, Instant.now(), Instant.now());

    given(reviewService.createReview(any(ReviewCreateRequest.class))).willReturn(response);

    mockMvc.perform(post("/api/reviews")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.bookId").value(bookId.toString()))
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
}