package com.team3.deokhugam.controller.book.docs;

import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.exception.global.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.http.MediaType;

@Retention(RetentionPolicy.RUNTIME)
@Operation(
    summary = "도서 상세 조회",
    description = "도서 ID로 도서 상세 정보를 조회합니다."
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "도서 상세 조회 성공",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = BookDto.class)
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
public @interface BookFindByIdApi {
}