package com.team3.deokhugam.controller.book.docs;

import com.team3.deokhugam.exception.global.ErrorResponse;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.springframework.http.MediaType;

@Retention(RetentionPolicy.RUNTIME)
@Operation(
    summary = "도서 목록 조회",
    description = "키워드, 정렬 조건, 커서 페이지네이션 조건으로 도서 목록을 조회합니다."
)
@Parameters({
    @Parameter(name = "keyword", description = "도서 제목, 저자, ISBN 검색어"),
    @Parameter(name = "orderBy", description = "정렬 기준: title, publishedDate, rating, reviewCount"),
    @Parameter(name = "direction", description = "정렬 방향: ASC, DESC"),
    @Parameter(name = "cursor", description = "다음 페이지 조회를 위한 커서 값"),
    @Parameter(name = "after", description = "다음 페이지 조회를 위한 보조 커서 시간"),
    @Parameter(name = "limit", description = "조회할 도서 개수")
})
@ApiResponses({
    @ApiResponse(
        responseCode = "200",
        description = "도서 목록 조회 성공",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = CursorPageResponse.class)
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
        responseCode = "500",
        description = "서버 내부 오류",
        content = @Content(
            mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)
        )
    )
})
public @interface BookSearchApi {
}