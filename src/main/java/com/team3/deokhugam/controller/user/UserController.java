package com.team3.deokhugam.controller.user;

import com.team3.deokhugam.controller.user.docs.UserFindByIdApi;
import com.team3.deokhugam.controller.user.docs.UserRegisterApi;
import com.team3.deokhugam.controller.user.docs.UserUpdateApi;
import com.team3.deokhugam.dto.user.UserRegisterRequest;
import com.team3.deokhugam.dto.user.UserDto;
import com.team3.deokhugam.dto.user.UserUpdateRequest;
import com.team3.deokhugam.service.user.UserService;
import com.team3.deokhugam.controller.user.docs.UserLoginApi;
import com.team3.deokhugam.dto.user.UserLoginRequest;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 관리", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

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
}
