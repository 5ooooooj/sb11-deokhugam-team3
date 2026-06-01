package com.team3.deokhugam.controller.book.docs;

import com.team3.deokhugam.dto.book.BookDto;
import com.team3.deokhugam.exception.global.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Encoding;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.http.MediaType;

@Retention(RetentionPolicy.RUNTIME)
@Operation(
    summary = "도서 수정",
    description = """
        도서 정보를 수정합니다. ISBN은 수정할 수 없습니다.
        
        현재 썸네일 URL은 bookData.thumbnailUrl 값을 기준으로 수정합니다.
        thumbnailImage 파일 업로드 후 URL로 변환하는 기능은 S3 업로드 연동 작업에서 처리할 예정입니다.
        """,
    requestBody = @RequestBody(
        required = true,
        content = @Content(
            mediaType = MediaType.MULTIPART_FORM_DATA_VALUE,
            encoding = {
                @Encoding(name = "bookData", contentType = MediaType.APPLICATION_JSON_VALUE),
                @Encoding(name = "thumbnailImage", contentType = MediaType.APPLICATION_OCTET_STREAM_VALUE)
            }
        )
    )
)
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "도서 수정 성공",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = BookDto.class)
        )
    ),
    @ApiResponse(
        responseCode = "400",
        description = "잘못된 요청",
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
public @interface BookUpdateApi {
}