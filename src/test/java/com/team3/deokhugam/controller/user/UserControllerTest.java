package com.team3.deokhugam.controller.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.mockito.BDDMockito.willThrow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.dto.dashboard.PowerUserDto;
import com.team3.deokhugam.dto.user.UserRegisterRequest;
import com.team3.deokhugam.dto.user.UserDto;
import com.team3.deokhugam.dto.user.UserUpdateRequest;
import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;
import com.team3.deokhugam.exception.user.UserNotFoundException;
import com.team3.deokhugam.global.dto.CursorPageResponse;
import com.team3.deokhugam.service.dashboard.PowerUserService;
import com.team3.deokhugam.service.user.UserService;
import com.team3.deokhugam.dto.user.UserLoginRequest;
import com.team3.deokhugam.exception.user.LoginFailedException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import com.team3.deokhugam.exception.user.UserForbiddenException;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

  @MockitoBean
  private PowerUserService powerUserService;

  @Test
  void register_success() throws Exception {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "test@test.test",
        "tester",
        "Password1!"
    );

    UserDto response = new UserDto(
        UUID.randomUUID(),
        request.email(),
        request.nickname(),
        Instant.now()
    );

    given(userService.register(any(UserRegisterRequest.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/users")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.email").value(response.email()))
        .andExpect(jsonPath("$.nickname").value(response.nickname()));

    verify(userService).register(any(UserRegisterRequest.class));
  }

  @Test
  void login_success() throws Exception {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "test@test.com",
        "Password1!"
    );

    UserDto response = new UserDto(
        UUID.randomUUID(),
        request.email(),
        "tester",
        Instant.now()
    );

    given(userService.login(any(UserLoginRequest.class)))
        .willReturn(response);

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.email").value(response.email()))
        .andExpect(jsonPath("$.nickname").value(response.nickname()));

    verify(userService).login(any(UserLoginRequest.class));
  }

  @Test
  void login_fail_invalidRequest() throws Exception {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "invalid-email",
        ""
    );

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

    verifyNoInteractions(userService);
  }

  @Test
  void login_fail_unauthorized() throws Exception {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "test@test.com",
        "WrongPassword1!"
    );

    given(userService.login(any(UserLoginRequest.class)))
        .willThrow(new LoginFailedException());

    // when & then
    mockMvc.perform(post("/api/users/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isUnauthorized())
        .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("로그인에 실패했습니다."))
        .andExpect(jsonPath("$.details").value("로그인에 실패했습니다."));

    verify(userService).login(any(UserLoginRequest.class));
  }

  @Test
  void findUserById_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    UserDto response = new UserDto(
        userId,
        "test@test.com",
        "tester",
        Instant.now()
    );
    given(userService.findUserById(userId))
        .willReturn(response);

    // when, then
    mockMvc.perform(get("/api/users/{userId}", userId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.email").value(response.email()))
        .andExpect(jsonPath("$.nickname").value(response.nickname()));

    verify(userService).findUserById(userId);
  }

  @Test
  void findUserById_fail_notFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    given(userService.findUserById(userId))
        .willThrow(new UserNotFoundException());

    // when, then
    mockMvc.perform(get("/api/users/{userId}", userId)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));

    verify(userService).findUserById(userId);
  }

  @Test
  void updateUser_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    UserUpdateRequest request = new UserUpdateRequest(
        "newNickname"
    );

    UserDto response = new UserDto(
        userId,
        "test@test.com",
        request.nickname(),
        Instant.now()
    );

    // 무조건 response 반환
    given(userService.updateUser(any(UUID.class), any(UUID.class), any(UserUpdateRequest.class)))
        .willReturn(response);

    // when, then
    mockMvc.perform(patch("/api/users/{userId}", userId)
            .header("Deokhugam-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(response.id().toString()))
        .andExpect(jsonPath("$.email").value(response.email()))
        .andExpect(jsonPath("$.nickname").value("newNickname"));

    verify(userService).updateUser(any(UUID.class), any(UUID.class), any(UserUpdateRequest.class));
  }

  @Test
  void updateUser_fail_invalidRequest() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    UserUpdateRequest request = new UserUpdateRequest("");

    // when, then
    mockMvc.perform(patch("/api/users/{userId}", userId)
            .header("Deokhugam-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.message").value("잘못된 요청입니다."));

    verifyNoInteractions(userService);
  }

  @Test
  void updateUser_fail_notFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    UserUpdateRequest request = new UserUpdateRequest("newNickname");

    // 예외 반환
    given(userService.updateUser(any(UUID.class), any(UUID.class), any(UserUpdateRequest.class)))
        .willThrow(new UserNotFoundException());

    // when, then
    mockMvc.perform(patch("/api/users/{userId}", userId)
            .header("Deokhugam-Request-User-ID", userId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("사용자를 찾을 수 없습니다."));

    verify(userService).updateUser(any(UUID.class), any(UUID.class), any(UserUpdateRequest.class));
  }

  @Test
  void updateUser_fail_forbidden() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID loginUserId = UUID.randomUUID();

    UserUpdateRequest request = new UserUpdateRequest(
        "newNickname"
    );

    given(userService.updateUser(any(UUID.class), any(UUID.class), any(UserUpdateRequest.class)))
        .willThrow(new UserForbiddenException());

    // when, then
    mockMvc.perform(patch("/api/users/{userId}", userId)
            .header("Deokhugam-Request-User-ID", loginUserId.toString())
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"))
        .andExpect(jsonPath("$.status").value(403))
        .andExpect(jsonPath("$.message").value("사용자 정보에 접근할 권한이 없습니다."));

    verify(userService).updateUser(any(UUID.class), any(UUID.class), any(UserUpdateRequest.class));
  }

  @Test
  void deleteUser_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    // when, then
    mockMvc.perform(delete("/api/users/{userId}", userId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());

    verify(userService).deleteUser(userId, userId);
  }

  @Test
  void deleteUser_fail_forbidden() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID loginUserId = UUID.randomUUID();

    willThrow(new UserForbiddenException())
        .given(userService)
        .deleteUser(userId, loginUserId);

    // when, then
    mockMvc.perform(delete("/api/users/{userId}", userId)
            .header("Deokhugam-Request-User-ID", loginUserId.toString()))
        .andExpect(status().isForbidden());

    verify(userService).deleteUser(userId, loginUserId);
  }

  @Test
  void deleteUser_fail_notFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willThrow(new UserNotFoundException())
        .given(userService)
        .deleteUser(userId, userId);

    // when, then
    mockMvc.perform(delete("/api/users/{userId}", userId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));

    verify(userService).deleteUser(userId, userId);
  }

  @Test
  void deleteUser_fail_internalServerError() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willThrow(new RuntimeException("unexpected error"))
        .given(userService)
        .deleteUser(userId, userId);

    // when, then
    mockMvc.perform(delete("/api/users/{userId}", userId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.status").value(500));

    verify(userService).deleteUser(userId, userId);
  }

  @Test
  @DisplayName("성공: GET /api/users/power - 200 정상 응답")
  void getPowerUsers_success() throws Exception {
    List<PowerUserDto> content = List.of(
        new PowerUserDto(UUID.randomUUID(), "유저1", Period.DAILY, Instant.now(), 1,
            BigDecimal.valueOf(90), BigDecimal.valueOf(50), 10, 5),
        new PowerUserDto(UUID.randomUUID(), "유저2", Period.DAILY, Instant.now(), 2,
            BigDecimal.valueOf(80), BigDecimal.valueOf(40), 8, 3)
    );
    CursorPageResponse<PowerUserDto> mockResponse = new CursorPageResponse<>(
        content, null, null, content.size(), 2L, false
    );

    given(powerUserService.getPowerUsers(eq("DAILY"), eq(10)))
        .willReturn(mockResponse);

    mockMvc.perform(get("/api/users/power")
            .param("period", "DAILY")
            .param("limit", "10"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.content.length()").value(2))
        .andExpect(jsonPath("$.content[0].rank").value(1))
        .andExpect(jsonPath("$.hasNext").value(false));

    verify(powerUserService).getPowerUsers(eq("DAILY"), eq(10));
  }

  @Test
  @DisplayName("실패: GET /api/users/power - 잘못된 period → 400")
  void getPowerUsers_invalidPeriod_returns400() throws Exception {
    given(powerUserService.getPowerUsers(eq("INVALID"), anyInt()))
        .willThrow(new DeokhugamException(ErrorCode.INVALID_PERIOD));

    mockMvc.perform(get("/api/users/power")
            .param("period", "INVALID"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PERIOD"));
  }

  @Test
  @DisplayName("실패: GET /api/users/power - limit 0 → 400")
  void getPowerUsers_invalidLimit_returns400() throws Exception {
    given(powerUserService.getPowerUsers(anyString(), eq(0)))
        .willThrow(new DeokhugamException(ErrorCode.INVALID_INPUT));

    mockMvc.perform(get("/api/users/power")
            .param("limit", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
  }

}