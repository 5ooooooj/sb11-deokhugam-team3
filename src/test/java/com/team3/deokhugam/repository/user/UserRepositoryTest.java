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
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

class UserRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private UserRepository userRepository;

  @Autowired
  private EntityManager entityManager;

  @TestConfiguration
  @EnableJpaAuditing(dateTimeProviderRef = "dateTimeProvider")
  static class JpaAuditingTestConfig {

    @Bean
    DateTimeProvider dateTimeProvider() {
      return () -> Optional.of(Instant.now());
    }
  }

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
    assertThat(result.get().getNickname()).isEqualTo("tester");
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

  @Test
  @DisplayName("논리삭제 후 기준 시간이 지난 사용자만 물리삭제")
  void deleteExpired_success(){
    // given
    User activeUser = userRepository.saveAndFlush(new User(
        "active@test.com",
        "activeUser",
        "Password1!"
    ));

    User recentDeletedUser = userRepository.saveAndFlush(new User(
        "recent@test.com",
        "recentUser",
        "Password1!"
    ));
    recentDeletedUser.softDelete();

    User expiredDeletedUser = userRepository.saveAndFlush(new User(
        "expired@test.com",
        "expiredUser",
        "Password1!"
    ));
    expiredDeletedUser.softDelete();

    userRepository.flush();

    // 12시간전 삭제된 유저
    setDeletedAt(recentDeletedUser, Instant.now().minus(12, ChronoUnit.HOURS));
    // 2일전 삭제된 유저
    setDeletedAt(expiredDeletedUser, Instant.now().minus(2, ChronoUnit.DAYS));

    entityManager.flush();
    entityManager.clear();

    Instant deleteBefore = Instant.now().minus(1, ChronoUnit.DAYS);

    // when
    userRepository.deleteExpiredSoftDeletedUsers(deleteBefore);

    entityManager.flush();
    entityManager.clear();

    // then
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