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
    summary = "OCR 기반 ISBN 인식",
    description = "OCR을 통해 도서 이미지에서 ISBN을 인식합니다."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "ISBN 인식 성공",
        content = @Content(
            mediaType = MediaType.TEXT_PLAIN_VALUE,
            schema = @Schema(implementation = String.class)
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 이미지 형식 또는 OCR 인식 실패",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )
    ),
    @ApiResponse(
        responseCode = "500",
        description = "서버 내부 오류 또는 OCR API 호출 실패",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
})
public @interface BookIsbnOcrApi {
}