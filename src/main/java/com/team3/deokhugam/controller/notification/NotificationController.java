package com.team3.deokhugam.controller.notification;

import com.team3.deokhugam.controller.notification.docs.NotificationConfirmAllApi;
import com.team3.deokhugam.controller.notification.docs.NotificationConfirmApi;
import com.team3.deokhugam.controller.notification.docs.NotificationFindAllApi;
import com.team3.deokhugam.dto.notification.NotificationDto;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.notification.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "알림 관리", description = "알림 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/notifications")
public class NotificationController {

  private final NotificationService notificationService;

  @NotificationFindAllApi
  @GetMapping
  public ResponseEntity<CursorPageResponse<NotificationDto>> findAll(
      @RequestParam(defaultValue = "DESC") String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    CursorPageResponse<NotificationDto> response =
        notificationService.findAll(requestUserId, after, limit);
    return ResponseEntity.ok(response);
  }

  @NotificationConfirmApi
  @PatchMapping("/{notificationId}")
  public ResponseEntity<NotificationDto> confirm(
      @PathVariable UUID notificationId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    NotificationDto response =
        notificationService.confirm(notificationId, requestUserId);
    return ResponseEntity.ok(response);
  }

  @NotificationConfirmAllApi
  @PatchMapping("/read-all")
  public ResponseEntity<Void> confirmAll(
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    notificationService.confirmAll(requestUserId);
    return ResponseEntity.ok().build();
  }
}