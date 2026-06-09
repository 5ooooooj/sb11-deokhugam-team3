package com.team3.deokhugam.repository.dashboard;

import static org.assertj.core.api.Assertions.assertThat;

import com.team3.deokhugam.batch.global.Period;
import com.team3.deokhugam.domain.dashboard.PowerUser;
import com.team3.deokhugam.domain.user.User;
import com.team3.deokhugam.dto.dashboard.PowerUserDto;
import com.team3.deokhugam.repository.BaseRepositoryTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;

@DisplayName("PowerUser 레포지토리 테스트")
class PowerUserRepositoryTest extends BaseRepositoryTest {

  @Autowired
  private PowerUserRepository powerUserRepository;

  @Autowired
  private TestEntityManager em;

  private UUID userId1;
  private UUID userId2;

  @BeforeEach
  void setUp() {
    User user1 = em.persistAndFlush(new User("test1@test.com", "유저1", "encoded"));
    User user2 = em.persistAndFlush(new User("test2@test.com", "유저2", "encoded"));

    userId1 = user1.getId();
    userId2 = user2.getId();

    em.persist(PowerUser.builder()
        .userId(userId1)
        .period(Period.DAILY)
        .score(BigDecimal.valueOf(90))
        .ranking(1)
        .reviewScoreSum(BigDecimal.valueOf(50))
        .likeCount(10)
        .commentCount(5)
        .calculatedAt(Instant.now())
        .build());

    em.persist(PowerUser.builder()
        .userId(userId2)
        .period(Period.DAILY)
        .score(BigDecimal.valueOf(80))
        .ranking(2)
        .reviewScoreSum(BigDecimal.valueOf(40))
        .likeCount(8)
        .commentCount(3)
        .calculatedAt(Instant.now())
        .build());

    em.persist(PowerUser.builder()
        .userId(userId1)
        .period(Period.WEEKLY)
        .score(BigDecimal.valueOf(95))
        .ranking(1)
        .reviewScoreSum(BigDecimal.valueOf(60))
        .likeCount(15)
        .commentCount(7)
        .calculatedAt(Instant.now())
        .build());

    em.flush();
    em.clear();
  }

  @Test
  @DisplayName("성공: DAILY period 파워 유저 rank 오름차순 조회")
  void findPowerUsersByPeriod_daily_success() {
    List<PowerUserDto> result = powerUserRepository
        .findPowerUsersByPeriod(Period.DAILY, PageRequest.of(0, 10));

    assertThat(result).hasSize(2);
    assertThat(result.get(0).rank()).isEqualTo(1);
    assertThat(result.get(1).rank()).isEqualTo(2);
  }

  @Test
  @DisplayName("성공: period 필터 - DAILY 조회 시 WEEKLY 데이터 미포함")
  void findPowerUsersByPeriod_filtersByPeriod() {
    List<PowerUserDto> result = powerUserRepository
        .findPowerUsersByPeriod(Period.DAILY, PageRequest.of(0, 10));

    assertThat(result).allMatch(dto -> dto.period() == Period.DAILY);
  }

  @Test
  @DisplayName("성공: limit 적용 - 1개만 조회")
  void findPowerUsersByPeriod_limitApplied() {
    List<PowerUserDto> result = powerUserRepository
        .findPowerUsersByPeriod(Period.DAILY, PageRequest.of(0, 1));

    assertThat(result).hasSize(1);
    assertThat(result.get(0).rank()).isEqualTo(1);
  }

  @Test
  @DisplayName("성공: 데이터 없는 period 조회 시 빈 리스트 반환")
  void findPowerUsersByPeriod_emptyResult() {
    List<PowerUserDto> result = powerUserRepository
        .findPowerUsersByPeriod(Period.MONTHLY, PageRequest.of(0, 10));

    assertThat(result).isEmpty();
  }

  @Test
  @DisplayName("성공: User 닉네임이 함께 조회된다")
  void findPowerUsersByPeriod_includesNickname() {
    List<PowerUserDto> result = powerUserRepository
        .findPowerUsersByPeriod(Period.DAILY, PageRequest.of(0, 10));

    assertThat(result.get(0).nickname()).isEqualTo("유저1");
    assertThat(result.get(1).nickname()).isEqualTo("유저2");
  }

  @Test
  @DisplayName("성공: countByPeriod - DAILY 2개 반환")
  void countByPeriod_success() {
    int count = powerUserRepository.countByPeriod(Period.DAILY);

    assertThat(count).isEqualTo(2);
  }

  @Test
  @DisplayName("성공: countByPeriod - 데이터 없는 period는 0 반환")
  void countByPeriod_empty() {
    int count = powerUserRepository.countByPeriod(Period.MONTHLY);

    assertThat(count).isEqualTo(0);
  }
}