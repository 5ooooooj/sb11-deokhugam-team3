package com.team3.deokhugam.domain.user.service;

import com.team3.deokhugam.domain.user.dto.request.UserLoginRequest;
import com.team3.deokhugam.domain.user.dto.request.UserRegisterRequest;
import com.team3.deokhugam.domain.user.dto.response.UserDto;
import com.team3.deokhugam.domain.user.entity.User;
import com.team3.deokhugam.domain.user.repository.UserRepository;
import com.team3.deokhugam.global.exception.LoginFailedException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  @Transactional
  public UserDto register(UserRegisterRequest request) {
    if (userRepository.existsByEmail(request.email())) {
      throw new IllegalStateException("이미 사용중인 이메일입니다.");
    }
    String encodedPassword = passwordEncoder.encode(request.password());

    User user = new User(request.email(), request.nickname(), encodedPassword);
    User savedUser;
    try {
      savedUser = userRepository.saveAndFlush(user);
      return UserDto.from(savedUser);
    } catch (DataIntegrityViolationException e) {
      throw new IllegalStateException("이미 사용중인 이메일입니다.", e);
    }
  }

  public UserDto login(UserLoginRequest request) {
    User user = userRepository.findActiveByEmail(request.email())
        .orElseThrow(LoginFailedException::new);

    if(!passwordEncoder.matches(request.password(),user.getEncodedPassword())){
      throw new LoginFailedException();
    }

    return UserDto.from(user);
  }
}
