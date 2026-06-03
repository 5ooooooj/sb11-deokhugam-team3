package com.team3.deokhugam.repository.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.repository.BaseRepositoryTest;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class UserRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  @Test
  @DisplayName("이메일 존재 시 true")
  void existsByEmail_true() {
    User user = new User("test@test.com", "tester", "Password1!");
    userRepository.saveAndFlush(user);

    boolean result = userRepository.existsByEmail("test@test.com");

    assertThat(result).isTrue();
  }

  @Test
  @DisplayName("이메일 없으면 false")
  void existsByEmail_false() {
    boolean result = userRepository.existsByEmail("false@test.com");

    assertThat(result).isFalse();
  }

  @Test
  @DisplayName("deleteAt이 null인 사용자 이메일 조회 시 사용자 반환")
  void findActiveByEmail_success() {
    User user = new User("logintest@test.com", "tester", "Password1!");
    userRepository.saveAndFlush(user);

    Optional<User> result = userRepository.findActiveByEmail("logintest@test.com");

    assertThat(result).isPresent();
    assertThat(result.get().getEmail()).isEqualTo("logintest@test.com");
    assertThat(result.get().getNickname()).isEqualTo("tester");
  }

  @Test
  @DisplayName("존재하지 않는 이메일(deleteAt!=null 포함) 조회 시 빈 Optional반환")
  void findActiveByEmail_notFound() {
    Optional<User> result = userRepository.findActiveByEmail("loginNotFound@test.com");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("삭제된 사용자(soft포함)는 이메일 조회 X")
  void findActiveByEmail_deletedUser() {
    User user = new User("deletedtest@test.com", "tester", "Password1!");
    User savedUser = userRepository.saveAndFlush(user);
    savedUser.softDelete();
    userRepository.flush();

    Optional<User> result = userRepository.findActiveByEmail("deletedtest@test.com");

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("findActiveById로 조회 시 사용자 반환")
  void findActiveById_success() {
    User user = new User("id@test.com", "idUser", "encodedPassword");
    User savedUser = userRepository.saveAndFlush(user);

    Optional<User> result = userRepository.findActiveById(savedUser.getId());

    assertThat(result).isPresent();
    assertThat(result.get().getId()).isEqualTo(savedUser.getId());
    assertThat(result.get().getEmail()).isEqualTo("id@test.com");
  }

  @Test
  @DisplayName("삭제된 사용자는 id로 조회되지 않음")
  void findActiveById_deletedUser() {
    User user = new User("deleted-id@test.com", "deletedIdUser", "encodedPassword");
    User savedUser = userRepository.saveAndFlush(user);
    savedUser.softDelete();
    userRepository.flush();

    Optional<User> result = userRepository.findActiveById(savedUser.getId());

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("논리삭제 후 기준 시간이 지난 사용자만 물리삭제")
  void deleteExpired_success() {
    User activeUser = userRepository.saveAndFlush(
        new User("active@test.com", "activeUser", "Password1!"));

    User recentDeletedUser = userRepository.saveAndFlush(
        new User("recent@test.com", "recentUser", "Password1!"));
    recentDeletedUser.softDelete();

    User expiredDeletedUser = userRepository.saveAndFlush(
        new User("expired@test.com", "expiredUser", "Password1!"));
    expiredDeletedUser.softDelete();

    userRepository.flush();

    setDeletedAt(recentDeletedUser, Instant.now().minus(12, ChronoUnit.HOURS));
    setDeletedAt(expiredDeletedUser, Instant.now().minus(2, ChronoUnit.DAYS));

    entityManager.flush();
    entityManager.clear();

    Instant deleteBefore = Instant.now().minus(1, ChronoUnit.DAYS);

    userRepository.deleteExpiredSoftDeletedUsers(deleteBefore);

    entityManager.flush();
    entityManager.clear();

    assertThat(userRepository.findById(activeUser.getId())).isPresent();
    assertThat(userRepository.findById(recentDeletedUser.getId())).isPresent();
    assertThat(userRepository.findById(expiredDeletedUser.getId())).isEmpty();
  }

  private void setDeletedAt(User user, Instant deletedAt) {
    entityManager.createQuery("""
        update User u
        set u.deletedAt = :deletedAt
        where u.id = :id
        """)
        .setParameter("deletedAt", deletedAt)
        .setParameter("id", user.getId())
        .executeUpdate();
  }
}