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
import com.team3.deokhugam.dto.user.UserLoginRequest;
import com.team3.deokhugam.exception.user.LoginFailedException;
import java.util.Optional;
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

    given(userRepository.existsByEmail(request.email())).willReturn(false);

    given(userRepository.saveAndFlush(any(User.class)))
        .willAnswer(invocation -> invocation.getArgument(0));

    // when
    UserDto result = userService.register(request);

    // then
    assertThat(result.email()).isEqualTo(request.email());
    assertThat(result.nickname()).isEqualTo(request.nickname());

    ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
    then(userRepository).should().saveAndFlush(userCaptor.capture());

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

  @Test
  void login_success(){
    // given
    String rawPassword = "Password1!";
    String encodedPassword = passwordEncoder.encode(rawPassword);

    User user = new User(
        "test@test.com",
        "tester",
        encodedPassword
    );

    UserLoginRequest request = new UserLoginRequest(
        "test@test.com",
        rawPassword
    );

    given(userRepository.findActiveByEmail(request.email()))
        .willReturn(Optional.of(user));

    // when
    UserDto result = userService.login(request);

    // then
    assertThat(result.email()).isEqualTo(user.getEmail());
    assertThat(result.nickname()).isEqualTo(user.getNickname());

    then(userRepository).should().findActiveByEmail(request.email());
  }

  @Test
  void login_fail_email(){
    // given
    UserLoginRequest request = new UserLoginRequest(
        "notfound@test.com",
        "Password1!"
    );

    given(userRepository.findActiveByEmail(request.email()))
        .willReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> userService.login(request))
        .isInstanceOf(LoginFailedException.class)
        .hasMessage("로그인에 실패했습니다.");

    then(userRepository).should().findActiveByEmail(request.email());
  }

  @Test
  void login_fail_password(){
    // given
    User user = new User(
        "test@test.com",
        "tester",
        passwordEncoder.encode("Password1!")
    );

    UserLoginRequest request = new UserLoginRequest(
        "test@test.com",
        "Password2!"
    );

    given(userRepository.findActiveByEmail(request.email()))
        .willReturn(Optional.of(user));

    // when, then
    assertThatThrownBy(() -> userService.login(request))
        .isInstanceOf(LoginFailedException.class)
        .hasMessage("로그인에 실패했습니다.");

    then(userRepository).should().findActiveByEmail(request.email());
  }

}
