package com.team3.deokhugam.controller.user;

import com.team3.deokhugam.controller.user.docs.UserFindByIdApi;
import com.team3.deokhugam.controller.user.docs.UserRegisterApi;
import com.team3.deokhugam.controller.user.docs.UserSoftDeleteApi;
import com.team3.deokhugam.controller.user.docs.UserUpdateApi;
import com.team3.deokhugam.dto.dashboard.PowerUserDto;
import com.team3.deokhugam.dto.user.UserRegisterRequest;
import com.team3.deokhugam.dto.user.UserDto;
import com.team3.deokhugam.dto.user.UserUpdateRequest;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.dashboard.PowerUserService;
import com.team3.deokhugam.service.user.UserService;
import com.team3.deokhugam.controller.user.docs.UserLoginApi;
import com.team3.deokhugam.dto.user.UserLoginRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 관리", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;
  private final PowerUserService powerUserService;

  @UserRegisterApi
  @PostMapping
  public ResponseEntity<UserDto> register(
      @Valid @RequestBody UserRegisterRequest request
  ) {
    UserDto response = userService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }

  @UserLoginApi
  @PostMapping("/login")
  public ResponseEntity<UserDto> login(
      @Valid @RequestBody UserLoginRequest request
  ) {
    UserDto response = userService.login(request);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @UserFindByIdApi
  @GetMapping("/{userId}")
  public ResponseEntity<UserDto> findUserById(@PathVariable UUID userId) {
    UserDto response = userService.findUserById(userId);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @UserUpdateApi
  @PatchMapping("/{userId}")
  public ResponseEntity<UserDto> updateUser(@PathVariable UUID userId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID loginUserId,
      @Valid @RequestBody UserUpdateRequest request) {
    UserDto response = userService.updateUser(userId, loginUserId, request);
    return ResponseEntity.status(HttpStatus.OK).body(response);
  }

  @UserSoftDeleteApi
  @DeleteMapping("/{userId}")
  public ResponseEntity<Void> softDeleteUser(@PathVariable UUID userId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID loginUserId) {
    userService.deleteUser(userId, loginUserId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }

  @Operation(summary = "파워 유저 목록 조회", description = "기간별 파워 유저 목록을 조회합니다")
  @ApiResponses({
      @ApiResponse(responseCode = "200", description = "파워 유저 목록 조회 성공"),
      @ApiResponse(responseCode = "400", description = "잘못된 요청 (랭킹 기간 오류)"),
      @ApiResponse(responseCode = "500", description = "서버 내부 오류")
  })
  @GetMapping("/power")
  public CursorPageResponse<PowerUserDto> getPowerUsers(
      @RequestParam(defaultValue = "ALL_TIME") String period,
      @RequestParam(required = false) String direction,
      @RequestParam(required = false) String cursor,
      @RequestParam(required = false) Instant after,
      @RequestParam(defaultValue = "50") int limit
  ) {
    return powerUserService.getPowerUsers(period, limit);
  }
}
