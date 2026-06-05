package com.team3.deokhugam.controller.user;

import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.team3.deokhugam.exception.user.UserForbiddenException;
import com.team3.deokhugam.exception.user.UserNotFoundException;
import com.team3.deokhugam.service.user.UserService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserHardDeleteController.class)
@ActiveProfiles("test")
public class UserHardDeleteControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private UserService userService;

  @Test
  void hardDeleteUser_success() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    // when, then
    mockMvc.perform(delete("/api/users/{userId}/hard", userId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isNoContent());

    verify(userService).hardDeleteUser(userId, userId);
  }

  @Test
  void hardDeleteUser_fail_forbidden() throws Exception {
    // given
    UUID userId = UUID.randomUUID();
    UUID loginUserId = UUID.randomUUID();

    willThrow(new UserForbiddenException())
        .given(userService)
        .hardDeleteUser(userId, loginUserId);

    // when, then
    mockMvc.perform(delete("/api/users/{userId}/hard", userId)
            .header("Deokhugam-Request-User-ID", loginUserId.toString()))
        .andExpect(status().isForbidden())
        .andExpect(jsonPath("$.code").value("USER_FORBIDDEN"))
        .andExpect(jsonPath("$.status").value(403));

    verify(userService).hardDeleteUser(userId, loginUserId);
  }

  @Test
  void hardDeleteUser_fail_notFound() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willThrow(new UserNotFoundException())
        .given(userService)
        .hardDeleteUser(userId, userId);

    // when, then
    mockMvc.perform(delete("/api/users/{userId}/hard", userId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("USER_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404));

    verify(userService).hardDeleteUser(userId, userId);
  }

  @Test
  void hardDeleteUser_fail_internalServerError() throws Exception {
    // given
    UUID userId = UUID.randomUUID();

    willThrow(new RuntimeException("unexpected error"))
        .given(userService)
        .hardDeleteUser(userId, userId);

    // when, then
    mockMvc.perform(delete("/api/users/{userId}/hard", userId)
            .header("Deokhugam-Request-User-ID", userId.toString()))
        .andExpect(status().isInternalServerError())
        .andExpect(jsonPath("$.code").value("INTERNAL_SERVER_ERROR"))
        .andExpect(jsonPath("$.status").value(500));

    verify(userService).hardDeleteUser(userId, userId);
  }
}
