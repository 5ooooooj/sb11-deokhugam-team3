package com.team3.deokhugam.repository.user;

import com.team3.deokhugam.domain.user.User;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("local")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class UserRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Test
  @DisplayName("이메일 존재 시 true")
  void existsByEmail_true() {
    // given
    User user = new User(
        "test@test.com",
        "tester",
        "Password1!"
    );
    userRepository.saveAndFlush(user);

    // when
    boolean result = userRepository.existsByEmail("test@test.com");

    // then
    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("이메일 없으면 false")
  void existsByEmail_false() {
    // when
    boolean result = userRepository.existsByEmail("false@test.com");

    // then
    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("deleteAt이 null인 사용자 이메일 조회 시 사용자 반환")
  void findActiveByEmail_success() {
    // given
    User user = new User(
        "logintest@test.com",
        "tester",
        "Password1!"
    );
    userRepository.saveAndFlush(user);

    // when
    Optional<User> result = userRepository.findActiveByEmail("logintest@test.com");

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("logintest@test.com");
    assertThat(result.get().getNickname()).isEqualTo("logintest");
  }

  @Test
  @DisplayName("존재하지 않는 이메일(deleteAt!=null 포함) 조회 시 빈 Optional반환")
  void findActiveByEmail_notFound() {
    // when
    Optional<User> result = userRepository.findActiveByEmail("loginNotFound@test.com");

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("삭제된 사용자(soft포함)는 이메일 조회 X")
  void findActiveByEmail_deletedUser() {
    // given
    User user = new User(
        "deletedtest@test.com",
        "tester",
        "Password1!"
    );
    User savedUser = userRepository.saveAndFlush(user);
    savedUser.softDelete();
    userRepository.flush();

    // when
    Optional<User> result = userRepository.findActiveByEmail("deletedtest@test.com");

    // then
    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findActiveById로 조회 시 사용자 반환")
  void findActiveById_success() {
    // given
    User user = new User(
        "id@test.com",
        "idUser",
        "encodedPassword"
    );
    User savedUser = userRepository.saveAndFlush(user);

    // when
    Optional<User> result = userRepository.findActiveById(savedUser.getId());

    // then
    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(savedUser.getId());
    assertThat(result.get().getEmail()).isEqualTo("id@test.com");
  }

  @Test
  @DisplayName("삭제된 사용자는 id로 조회되지 않음")
  void findActiveById_deletedUser() {
    // given
    User user = new User(
        "deleted-id@test.com",
        "deletedIdUser",
        "encodedPassword"
    );
    User savedUser = userRepository.saveAndFlush(user);

    savedUser.softDelete();
    userRepository.flush();

    // when
    Optional<User> result = userRepository.findActiveById(savedUser.getId());

    // then
    assertThat(result).isEmpty();
  }
}
