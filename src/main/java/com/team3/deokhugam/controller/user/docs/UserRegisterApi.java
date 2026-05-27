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
@Operation(summary = "회원가입", description = "새로운 사용자를 등록합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "201", description = "회원가입 성공", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = UserDto.class))),
    @ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 검증 실패)", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "409", description = "이메일 중복", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class))),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ErrorResponse.class)))})
public @interface UserRegisterApi {

}