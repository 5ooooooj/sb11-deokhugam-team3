package com.team3.deokhugam.domain.user.dto.request;

import static org.assertj.core.api.Assertions.assertThat;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

public class UserRegisterRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void validateEmail(){
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "test@test.test",
        "tester",
        "Password1!"
    );

    // when
    // 검증에 실패한것들 violations에 들어감
    var violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  void invalidEmail(){
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "test-test",
        "tester",
        "Password1!"
    );

    //when
    var violations = validator.validate(request);

    //then
    assertThat(violations).isNotEmpty();
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
  }

  @Test
  void invalidNickname() {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "test@test.com",
        "a",
        "Password1!"
    );

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("nickname"));
  }

  @Test
  void invalidPassword() {
    // given
    UserRegisterRequest request = new UserRegisterRequest(
        "test@test.com",
        "tester",
        "password"
    );

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("password"));
  }
}
