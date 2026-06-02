package com.team3.deokhugam.controller.comment;

import com.team3.deokhugam.dto.comment.CommentCreateRequest;
import com.team3.deokhugam.dto.comment.CommentDto;
import com.team3.deokhugam.dto.comment.CommentUpdateRequest;
import com.team3.deokhugam.exception.comment.CommentForbiddenException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.comment.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

@Tag(name = "댓글 관리", description = "댓글 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentController {

  private final CommentService commentService;

  @PostMapping
  public ResponseEntity<CommentDto> create(
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody CommentCreateRequest request
  ) {
    if (!requestUserId.equals(request.userId())) {
      throw new CommentForbiddenException();
    }
    CommentDto response = commentService.create(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @PatchMapping("/{commentId}")
  public ResponseEntity<CommentDto> update(
      @PathVariable UUID commentId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId,
      @Valid @RequestBody CommentUpdateRequest request
  ) {
    CommentDto response = commentService.update(commentId, requestUserId, request);
    return ResponseEntity.ok(response);
  }

  @DeleteMapping("/{commentId}")
  public ResponseEntity<Void> delete(
      @PathVariable UUID commentId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    commentService.delete(commentId, requestUserId);
    return ResponseEntity.noContent().build();
  }

  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDelete(
      @PathVariable UUID commentId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    commentService.hardDelete(commentId, requestUserId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<CursorPageResponse<CommentDto>> findAll(
      @RequestParam UUID reviewId,
      @RequestParam(defaultValue = "DESC") String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int limit,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    CursorPageResponse<CommentDto> response =
        commentService.findAll(reviewId, after, limit);
    return ResponseEntity.ok(response);
  }

  @GetMapping("/{commentId}")
  public ResponseEntity<CommentDto> findById(
      @PathVariable UUID commentId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    CommentDto response = commentService.findById(commentId);
    return ResponseEntity.ok(response);
  }
}