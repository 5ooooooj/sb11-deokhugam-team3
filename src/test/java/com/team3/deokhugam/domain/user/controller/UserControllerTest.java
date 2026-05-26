package com.team3.deokhugam.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.domain.user.dto.request.UserLoginRequest;
import com.team3.deokhugam.domain.user.dto.request.UserRegisterRequest;
import com.team3.deokhugam.domain.user.dto.response.UserDto;
import com.team3.deokhugam.domain.user.service.UserService;
import com.team3.deokhugam.global.exception.LoginFailedException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private ObjectMapper objectMapper;

  @MockitoBean
  private UserService userService;

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

    then(userService).should().register(any(UserRegisterRequest.class));
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

    then(userService).should().login(any(UserLoginRequest.class));
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

    then(userService).shouldHaveNoInteractions();
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
        .andExpect(jsonPath("$.status").value(401))
        .andExpect(jsonPath("$.message").value("로그인에 실패했습니다."))
        .andExpect(jsonPath("$.details").value("이메일 또는 비밀번호가 불일치합니다."));

    then(userService).should().login(any(UserLoginRequest.class));
  }
}