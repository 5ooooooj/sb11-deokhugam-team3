package com.team3.deokhugam.domain.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.team3.deokhugam.domain.user.dto.request.UserRegisterRequest;
import com.team3.deokhugam.domain.user.dto.response.UserDto;
import com.team3.deokhugam.domain.user.service.UserService;
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
}