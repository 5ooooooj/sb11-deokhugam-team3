package com.team3.deokhugam.domain.user.dto.request;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class UserLoginRequestTest {

  private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

  @Test
  void validateLoginEmail() {
    // given
    UserLoginRequest request = new UserLoginRequest(
        "test@test.com",
        "Password1!"
    );

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).isEmpty();
  }

  @Test
  void blankEmail(){
    // given
    UserLoginRequest request = new UserLoginRequest(
        "",
        "Password1!"
    );

    // when
    var violations = validator.validate(request);

    // then
    assertThat(violations).isNotEmpty();
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
  }

  @Test
  void invalidEmail(){
    // given
    UserLoginRequest request = new UserLoginRequest(
        "test-test",
        "Password1!"
    );

    // when
    var violations = validator.validate(request);

    //then
    assertThat(violations).isNotEmpty();
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("email"));
  }

  @Test
  void blankPassword(){
    // given
    UserLoginRequest request = new UserLoginRequest(
        "test@test.com",
        ""
    );

    // when
    var violations = validator.validate(request);

    //then
    assertThat(violations).isNotEmpty();
    assertThat(violations)
        .anyMatch(v -> v.getPropertyPath().toString().equals("password"));


  }
}
