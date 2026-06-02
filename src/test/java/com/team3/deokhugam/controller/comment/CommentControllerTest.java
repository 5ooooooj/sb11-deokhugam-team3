package com.team3.deokhugam.controller.comment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.dto.comment.CommentCreateRequest;
import com.team3.deokhugam.dto.comment.CommentDto;
import com.team3.deokhugam.dto.comment.CommentUpdateRequest;
import com.team3.deokhugam.exception.comment.CommentForbiddenException;
import com.team3.deokhugam.exception.comment.CommentNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.comment.CommentService;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@WebMvcTest(CommentController.class)
class CommentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private CommentService commentService;

  @Test
  void create_success() throws Exception {
    // given
    UUID requestUserId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    CommentCreateRequest request = new CommentCreateRequest(
        reviewId, requestUserId, "좋은 리뷰네요"
    );
    CommentDto response = new CommentDto(
        UUID.randomUUID(), reviewId, requestUserId,
        "테스트유저", "좋은 리뷰네요", Instant.now(), Instant.now()
    );
    given(commentService.create(any())).willReturn(response);

    // when & then
    mockMvc.perform(post("/api/comments")
            .header("Deokhugam-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").exists())
        .andExpect(jsonPath("$.content").value("좋은 리뷰네요"));

    verify(commentService).create(any());
  }

  @Test
  void update_success() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정된 내용");
    CommentDto response = new CommentDto(
        commentId, UUID.randomUUID(), requestUserId,
        "테스트유저", "수정된 내용", Instant.now(), Instant.now()
    );
    given(commentService.update(any(), any(), any())).willReturn(response);

    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", commentId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").value("수정된 내용"));

    verify(commentService).update(any(), any(), any());
  }

  @Test
  void update_forbidden() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();
    CommentUpdateRequest request = new CommentUpdateRequest("수정 시도");

    given(commentService.update(any(), any(), any()))
        .willThrow(new CommentForbiddenException());

    // when & then
    mockMvc.perform(patch("/api/comments/{commentId}", commentId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"))
        .andExpect(jsonPath("$.status").value(403));

    verify(commentService).update(any(), any(), any());
  }

  @Test
  void delete_success() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}", commentId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isNoContent());

    verify(commentService).delete(any(), any());
  }

  @Test
  void delete_forbidden() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    willThrow(new CommentForbiddenException())
        .given(commentService).delete(any(), any());

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}", commentId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("COMMENT_FORBIDDEN"))
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void findById_success() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();
    CommentDto response = new CommentDto(
        commentId, UUID.randomUUID(), requestUserId,
        "테스트유저", "댓글 내용", Instant.now(), Instant.now()
    );
    given(commentService.findById(any())).willReturn(response);

    // when & then
    mockMvc.perform(get("/api/comments/{commentId}", commentId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(commentId.toString()));

    verify(commentService).findById(any());
  }

  @Test
  void findById_notFound() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    given(commentService.findById(any()))
        .willThrow(new CommentNotFoundException());

    // when & then
    mockMvc.perform(get("/api/comments/{commentId}", commentId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("COMMENT_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void findAll_success() throws Exception {
    // given
    UUID requestUserId = UUID.randomUUID();
    UUID reviewId = UUID.randomUUID();
    CursorPageResponse<CommentDto> response = new CursorPageResponse<>(
        List.of(), null, null, 10, 0L, false
    );
    given(commentService.findAll(any(UUID.class), any(), any(Integer.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(get("/api/comments")
            .header("Deokhugam-Request-User-ID", requestUserId.toString())
            .param("reviewId", reviewId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(false));

    verify(commentService).findAll(any(UUID.class), any(), any(Integer.class));
  }
  @Test
  @DisplayName("댓글 물리 삭제 성공")
  void hardDelete_success() throws Exception {
    // given
    UUID commentId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    // when & then
    mockMvc.perform(delete("/api/comments/{commentId}/hard", commentId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isNoContent());

    verify(commentService).hardDelete(any(UUID.class), any(UUID.class));
  }
}