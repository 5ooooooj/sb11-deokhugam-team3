package com.team3.deokhugam.service.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;

import com.team3.deokhugam.dto.user.UserRegisterRequest;
import com.team3.deokhugam.dto.user.UserDto;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class UserServiceTest {

  private UserRepository userRepository;
  private PasswordEncoder passwordEncoder;
  private UserService userService;

  @BeforeEach
  void setUp() {
    userRepository = Mockito.mock(UserRepository.class);
    passwordEncoder = new BCryptPasswordEncoder();
    userService = new UserService(userRepository, passwordEncoder);
  }

  @Test
  void register_success() {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "test@test.test",
        "tester",
        "Password1!"
    );
    // 이메일 중복 false 반환
    given(userRepository.existsByEmail(request.email())).willReturn(false);
    // User 객체 그대로 반환
    given(userRepository.save(any(User.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    UserDto result = userService.register(request);

    // then
    assertThat(result.email()).isEqualTo(request.email());
    assertThat(result.nickname()).isEqualTo(request.nickname());

    // save 호출 확인
    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    then(userRepository).should().save(userCaptor.capture());

    User savedUser = userCaptor.getValue();

    assertThat(savedUser.getEmail()).isEqualTo(request.email());
    assertThat(savedUser.getNickname()).isEqualTo(request.nickname());
    assertThat(savedUser.getEncodedPassword()).isNotEqualTo(request.password());
    assertThat(passwordEncoder.matches(request.password(), savedUser.getEncodedPassword()))
        .isTrue();
  }

  @Test
  void register_fail() {
    //given
    UserRegisterRequest request = new UserRegisterRequest(
        "test@test.test",
        "tester",
        "Password1!"
    );

    // 이메일 중복 true 반환
    given(userRepository.existsByEmail(request.email())).willReturn(true);

    // when, then
    assertThatThrownBy(() -> userService.register(request))
        .isInstanceOf(IllegalStateException.class);

    then(userRepository).should().existsByEmail(request.email());
    then(userRepository).should(never()).save(any(User.class));
  }
}
