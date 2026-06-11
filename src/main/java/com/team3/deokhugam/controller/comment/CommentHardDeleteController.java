package com.team3.deokhugam.controller.comment;

import com.team3.deokhugam.controller.comment.docs.CommentHardDeleteApi;
import com.team3.deokhugam.service.comment.CommentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "댓글 관리", description = "댓글 관련 API")
@Profile({"local", "test"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/comments")
public class CommentHardDeleteController {

  private final CommentService commentService;

  @CommentHardDeleteApi
  @DeleteMapping("/{commentId}/hard")
  public ResponseEntity<Void> hardDelete(
      @PathVariable UUID commentId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID requestUserId
  ) {
    commentService.hardDelete(commentId, requestUserId);
    return ResponseEntity.noContent().build();
  }
}
