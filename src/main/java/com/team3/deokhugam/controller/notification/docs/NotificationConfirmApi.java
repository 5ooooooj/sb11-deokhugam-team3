package com.team3.deokhugam.controller.notification.docs;

import com.team3.deokhugam.dto.notification.NotificationDto;
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
@Operation(summary = "알림 단건 읽음", description = "알림을 읽음 처리합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "읽음 처리 성공",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = NotificationDto.class))),
    @ApiResponse(responseCode = "403", description = "권한 없음",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "알림 없음",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류",
        content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
            schema = @Schema(implementation = ErrorResponse.class)))
})
public @interface NotificationConfirmApi {}
