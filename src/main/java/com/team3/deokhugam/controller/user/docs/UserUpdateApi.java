package com.team3.deokhugam.controller.user.docs;

import com.team3.deokhugam.dto.user.UserDto;
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
@Operation(summary = "사용자 정보 수정", description = "사용자의 닉네임을 수정합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "200", description = "사용자 정보 수정 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "403", description = "사용자 정보 수정 권한 없음", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "404", description = "사용자 정보 없음", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))})
public @interface UserUpdateApi {

}
