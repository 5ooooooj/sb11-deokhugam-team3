package com.team3.deokhugam.domain.user.dto.response;

import com.team3.deokhugam.domain.user.entity.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserDtoTest {

  @Test
  void from() {
    // given
    User user = new User(
        "test@test.test",
        "tester",
        "Password1"
    );

    // when
    UserDto dto = UserDto.from(user);

    // then
    assertThat(dto.id()).isEqualTo(user.getId());
    assertThat(dto.email()).isEqualTo(user.getEmail());
    assertThat(dto.nickname()).isEqualTo(user.getNickname());
    assertThat(dto.createdAt()).isEqualTo(user.getCreatedAt());
  }
}
