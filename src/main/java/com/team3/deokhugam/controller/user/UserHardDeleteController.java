package com.team3.deokhugam.controller.user;

import com.team3.deokhugam.controller.user.docs.UserHardDeleteApi;
import com.team3.deokhugam.service.user.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 관리", description = "사용자 관련 API")
@Profile({"local", "test"})
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserHardDeleteController {

  private final UserService userService;

  @UserHardDeleteApi
  @DeleteMapping("/{userId}/hard")
  public ResponseEntity<Void> hardDeleteUser(@PathVariable UUID userId,
      @RequestHeader("Deokhugam-Request-User-ID") UUID loginUserId) {
    userService.hardDeleteUser(userId, loginUserId);
    return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
  }
}