package com.team3.deokhugam.dto.comment;

import java.util.UUID;

public record CommentCreateRequest(
    UUID reviewId,
    UUID userId,
    String content
) {}