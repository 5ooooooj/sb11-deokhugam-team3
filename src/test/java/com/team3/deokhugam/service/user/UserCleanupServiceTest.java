package com.team3.deokhugam.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.repository.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserCleanupServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  private Clock clock;

  @InjectMocks
  private UserCleanupService userCleanupService;

  @Test
  void hardDeleteExpiredUsers_success() {
    // given
    // 하루 뒤 시간 미리 넣기
    Instant now = Instant.parse("2026-05-28T00:00:00Z");
    Instant cutoff = Instant.parse("2026-05-27T00:00:00Z");

    given(clock.instant()).willReturn(now);
    given(clock.getZone()).willReturn(ZoneOffset.UTC);
    given(userRepository.deleteByDeletedAtLessThanEqual(cutoff))
        .willReturn(3);

    // when
    int result = userCleanupService.hardDeleteExpiredUsers();

    // then
    assertThat(result).isEqualTo(3);
    verify(userRepository).deleteByDeletedAtLessThanEqual(cutoff);
  }
}
