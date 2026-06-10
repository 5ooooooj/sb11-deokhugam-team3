package com.team3.deokhugam.controller.user.docs;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
@Operation(summary = "사용자 물리 삭제", description = "사용자를 물리적으로 삭제합니다.")
@ApiResponses({
    @ApiResponse(responseCode = "204", description = "사용자 물리 삭제 성공"),
    @ApiResponse(responseCode = "403", description = "사용자 삭제 권한 없음"),
    @ApiResponse(responseCode = "404", description = "사용자 정보 없음"),
    @ApiResponse(responseCode = "500", description = "서버 내부 오류")
})
public @interface UserHardDeleteApi {
}
