package com.team3.deokhugam.service.user;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.dto.user.UserRegisterRequest;
import com.team3.deokhugam.dto.user.UserDto;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.exception.user.EmailAlreadyExistsException;
import com.team3.deokhugam.exception.user.UserNotFoundException;
import com.team3.deokhugam.repository.user.UserRepository;
import com.team3.deokhugam.dto.user.UserLoginRequest;
import com.team3.deokhugam.exception.user.LoginFailedException;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  @Test
  void register_success() {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "test@test.test",
        "tester",
        "Password1!"
    );

    given(userRepository.existsByEmail(request.email())).willReturn(false);
    given(passwordEncoder.encode(request.password())).willReturn("encodedPassword");
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
    assertThat(savedUser.getEncodedPassword()).isEqualTo("encodedPassword");

    verify(userRepository).existsByEmail(request.email());
    verify(passwordEncoder).encode(request.password());
  }

  @Test
  void register_fail() {
    //given
    UserRegisterRequest request = new UserRegisterRequest(
        "test@test.test",
        "tester",
        "Password1!"
    );
    given(userRepository.existsByEmail(request.email())).willReturn(true);

    // when, then
    assertThatThrownBy(() -> userService.register(request))
        .isInstanceOf(EmailAlreadyExistsException.class);

    verify(userRepository).existsByEmail(request.email());
    verify(userRepository, never()).saveAndFlush(any(User.class));
    verify(passwordEncoder, never()).encode(any());
  }

  @Test
  void login_success(){
    // given
    User user = new User(
        "test@test.com",
        "tester",
        "encodedPassword"
    );

    UserLoginRequest request = new UserLoginRequest(
        "test@test.com",
        "Password1!"
    );

    given(userRepository.findActiveByEmail(request.email()))
        .willReturn(Optional.of(user));
    given(passwordEncoder.matches(request.password(), user.getEncodedPassword()))
        .willReturn(true);

    // when
    UserDto result = userService.login(request);

    // then
    assertThat(result.email()).isEqualTo(user.getEmail());
    assertThat(result.nickname()).isEqualTo(user.getNickname());

    verify(userRepository).findActiveByEmail(request.email());
    verify(passwordEncoder).matches(request.password(), user.getEncodedPassword());
  }

  @Test
  void login_fail_email() {
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

    verify(userRepository).findActiveByEmail(request.email());
    verify(passwordEncoder, never()).matches(any(), any());
  }

  @Test
  void login_fail_password() {
    // given
    User user = new User(
        "test@test.com",
        "tester",
        "encodedPassword"
    );

    UserLoginRequest request = new UserLoginRequest(
        "test@test.com",
        "Password2!"
    );

    given(userRepository.findActiveByEmail(request.email()))
        .willReturn(Optional.of(user));
    given(passwordEncoder.matches(request.password(), user.getEncodedPassword()))
        .willReturn(false);

    // when, then
    assertThatThrownBy(() -> userService.login(request))
        .isInstanceOf(LoginFailedException.class)
        .hasMessage("로그인에 실패했습니다.");

    verify(userRepository).findActiveByEmail(request.email());
    verify(passwordEncoder).matches(request.password(), user.getEncodedPassword());
  }

  @Test
  void findUserById_success(){
    // given
    UUID userId = UUID.randomUUID();

    User user = new User(
        "test@test.com",
        "tester",
        "Password1!"
    );
    given(userRepository.findActiveById(userId))
        .willReturn(Optional.of(user));

    // when
    UserDto result = userService.findUserById(userId);

    // then
    assertThat(result.email()).isEqualTo(user.getEmail());
    assertThat(result.nickname()).isEqualTo(user.getNickname());

    verify(userRepository).findActiveById(userId);
  }

  @Test
  void findUserById_fail_notFound(){
    // given
    UUID userId = UUID.randomUUID();

    given(userRepository.findActiveById(userId))
        .willReturn(Optional.empty());

    // when, then
    assertThatThrownBy(() -> userService.findUserById(userId))
        .isInstanceOf(UserNotFoundException.class);

    verify(userRepository).findActiveById(userId);
  }
}
