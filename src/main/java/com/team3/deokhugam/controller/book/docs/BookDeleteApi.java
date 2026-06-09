package com.team3.deokhugam.controller.book.docs;

import com.team3.deokhugam.exception.global.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.http.MediaType;

@Retention(RetentionPolicy.RUNTIME)
@Operation(
    summary = "도서 논리 삭제",
    description = "도서를 논리 삭제합니다. 등록한 사용자만 삭제할 수 있으며, 실제 데이터는 삭제하지 않고 deletedAt을 설정합니다."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "204",
        description = "도서 논리 삭제 성공"
    ),

    @ApiResponse(
        responseCode = "403",
        description = "도서 삭제 권한 없음",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )
    ),

    @ApiResponse(
        responseCode = "404",
        description = "도서 정보 없음",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
})
public @interface BookDeleteApi {
}