package com.team3.deokhugam.domain.user;

import java.time.Instant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

  @Test
  void createUser() {
    // given
    String email = "aa@naver.com";
    String nickname = "tester";
    String password = "encoded";

    // when
    User user = new User(email, nickname, password);

    // then
    assertThat(user.getEmail()).isEqualTo(email);
    assertThat(user.getNickname()).isEqualTo(nickname);
    assertThat(user.getEncodedPassword()).isEqualTo(password);
    assertThat(user.isDeleted()).isFalse();
  }

  @Test
  void updateNickname() {
    // given
    User user = new User("test@test.test", "기존닉네임", "encoded");

    // when
    user.updateNickname("새닉네임");

    // then
    assertThat(user.getNickname()).isEqualTo("새닉네임");
  }

  @Test
  void softDelete() {
    // given
    User user = new User("test@test.test", "기존닉네임", "encoded");

    // when
    user.softDelete();

    // then
    assertThat(user.isDeleted()).isTrue();
  }

  @Test
  void softDelete_doesNotOverwriteDeletedAt() {
    // given
    User user = new User("test@test.test", "기존닉네임", "encoded");
    user.softDelete();
    Instant firstDeletedAt = user.getDeletedAt();

    // when
    user.softDelete();

    // then
    assertThat(user.getDeletedAt()).isEqualTo(firstDeletedAt);
  }
}
