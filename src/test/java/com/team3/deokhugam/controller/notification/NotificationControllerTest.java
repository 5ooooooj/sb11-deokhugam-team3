package com.team3.deokhugam.controller.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.dto.notification.NotificationDto;
import com.team3.deokhugam.exception.notification.NotificationForbiddenException;
import com.team3.deokhugam.exception.notification.NotificationNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.notification.NotificationService;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@WebMvcTest(NotificationController.class)
class NotificationControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private NotificationService notificationService;

  @Test
  void findAll_success() throws Exception {
    // given
    UUID requestUserId = UUID.randomUUID();
    CursorPageResponse<NotificationDto> response = new CursorPageResponse<>(
        List.of(), null, null, 10, 0L, false
    );
    given(notificationService.findAll(any(UUID.class), any(), any(Integer.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(get("/api/notifications")
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.hasNext").value(false));

    verify(notificationService).findAll(any(UUID.class), any(), any(Integer.class));
  }

  @Test
  void confirm_success() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();
    NotificationDto response = new NotificationDto(
        notificationId, requestUserId, UUID.randomUUID(),
        null, "댓글이 달렸습니다.", null, true,
        Instant.now(), Instant.now()
    );
    given(notificationService.confirm(any(), any())).willReturn(response);

    // when & then
    mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(notificationId.toString()))
        .andExpect(jsonPath("$.confirmed").value(true));

    verify(notificationService).confirm(any(), any());
  }

  @Test
  void confirm_forbidden() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    given(notificationService.confirm(any(), any()))
        .willThrow(new NotificationForbiddenException());

    // when & then
    mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("NOTIFICATION_FORBIDDEN"))
        .andExpect(jsonPath("$.status").value(403));
  }

  @Test
  void confirm_notFound() throws Exception {
    // given
    UUID notificationId = UUID.randomUUID();
    UUID requestUserId = UUID.randomUUID();

    given(notificationService.confirm(any(), any()))
        .willThrow(new NotificationNotFoundException());

    // when & then
    mockMvc.perform(patch("/api/notifications/{notificationId}", notificationId)
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));
  }

  @Test
  void confirmAll_success() throws Exception {
    // given
    UUID requestUserId = UUID.randomUUID();

    // when & then
    mockMvc.perform(patch("/api/notifications/read-all")
            .header("Deokhugam-Request-User-ID", requestUserId.toString()))
        .andExpect(status().isOk());

    verify(notificationService).confirmAll(any());
  }
}