package com.team3.deokhugam.dto.comment;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CommentCreateRequest(
    @NotNull(message = "리뷰 ID는 필수입니다.")
    UUID reviewId,

    @NotNull(message = "사용자 ID는 필수입니다.")
    UUID userId,

    @NotBlank(message = "댓글 내용은 필수입니다.")
    @Size(max = 500, message = "댓글은 500자 이하로 입력해주세요.")
    String content
) {}
