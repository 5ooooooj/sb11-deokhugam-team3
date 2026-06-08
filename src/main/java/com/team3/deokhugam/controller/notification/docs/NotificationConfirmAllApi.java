package com.team3.deokhugam.controller.notification.docs;

import com.team3.deokhugam.exception.global.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.MediaType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "알림 전체 읽음", description = "사용자의 모든 알림을 읽음 처리합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "전체 읽음 처리 성공"),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
})
public @interface NotificationConfirmAllApi {}
