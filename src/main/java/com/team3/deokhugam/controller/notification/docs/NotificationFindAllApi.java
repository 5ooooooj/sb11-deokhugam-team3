package com.team3.deokhugam.controller.notification.docs;

import com.team3.deokhugam.exception.global.ErrorResponse;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 커서 페이지네이션으로 조회합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "알림 목록 조회 성공",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = CursorPageResponse.class))),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
})
public @interface NotificationFindAllApi {}
