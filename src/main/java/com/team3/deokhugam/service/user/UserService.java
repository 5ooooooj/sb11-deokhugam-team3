package com.team3.deokhugam.service.user;

import com.team3.deokhugam.dto.user.UserRegisterRequest;
import com.team3.deokhugam.dto.user.UserDto;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.exception.user.EmailAlreadyExistsException;
import com.team3.deokhugam.exception.user.UserNotFoundException;
import com.team3.deokhugam.repository.user.UserRepository;
import com.team3.deokhugam.dto.user.UserLoginRequest;
import com.team3.deokhugam.exception.user.LoginFailedException;
import java.util.UUID;
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
      throw new EmailAlreadyExistsException();
    }
    String encodedPassword = passwordEncoder.encode(request.password());

    User user = new User(request.email(), request.nickname(), encodedPassword);
    User savedUser;
    try {
      savedUser = userRepository.saveAndFlush(user);
      return UserDto.from(savedUser);
    } catch (DataIntegrityViolationException e) {
      throw new EmailAlreadyExistsException();
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

  public UserDto findUserById(UUID userId){
    User user = userRepository.findActiveById(userId)
        .orElseThrow(UserNotFoundException::new);

    return UserDto.from(user);
  }
}
