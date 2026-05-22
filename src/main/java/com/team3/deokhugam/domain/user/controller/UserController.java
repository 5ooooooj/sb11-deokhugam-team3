package com.team3.deokhugam.domain.user.controller;

import com.team3.deokhugam.domain.user.controller.docs.UserRegisterApi;
import com.team3.deokhugam.domain.user.dto.request.UserRegisterRequest;
import com.team3.deokhugam.domain.user.dto.response.UserDto;
import com.team3.deokhugam.domain.user.service.UserService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 관리", description = "사용자 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {

  private final UserService userService;

  @UserRegisterApi
  @PostMapping(
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE
  )
  public ResponseEntity<UserDto> register(
      @Valid @RequestBody UserRegisterRequest request
  ) {
    UserDto response = userService.register(request);
    return ResponseEntity.status(HttpStatus.CREATED).body(response);
  }
}
