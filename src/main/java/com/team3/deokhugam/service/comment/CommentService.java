package com.team3.deokhugam.service.comment;

import com.team3.deokhugam.dto.comment.CommentCreateRequest;
import com.team3.deokhugam.dto.comment.CommentDto;
import com.team3.deokhugam.dto.comment.CommentUpdateRequest;
import com.team3.deokhugam.global.dto.CursorPageResponse;

import java.time.Instant;
import java.util.UUID;

public interface CommentService {

  CommentDto create(CommentCreateRequest request);

  CommentDto update(UUID commentId, UUID requestUserId, CommentUpdateRequest request);

  void delete(UUID commentId, UUID requestUserId);

  void hardDelete(UUID commentId, UUID requestUserId);

  CommentDto findById(UUID commentId);

  CursorPageResponse<CommentDto> findAll(UUID reviewId, Instant after, int size);
}