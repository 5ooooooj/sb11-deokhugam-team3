package com.team3.deokhugam.domain.comment.dto.response;

import java.time.Instant;
import java.util.UUID;

public record CommentDto (
    UUID id,
    UUID reviewId,
    UUID userId,
    String userNickname,
    String content,
    Instant createdAt,
    Instant updatedAt
) {}
