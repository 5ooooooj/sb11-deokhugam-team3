package com.team3.deokhugam.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.repository.user.UserRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class UserCleanupServiceTest {

  @Mock
  private UserRepository userRepository;

  @InjectMocks
  private UserCleanupService userCleanupService;

  @Test
  void hardDeleteExpiredUsers_success() {
    // when
    userCleanupService.hardDeleteExpiredUsers();

    // then
    verify(userRepository).deleteExpiredSoftDeletedUsers(any(Instant.class));
  }

  @Test
  @DisplayName("UserCleanupService가 물리삭제 기준 시간으로 현재 시각 기준 하루 전을 사용하는지 확인")
  void usesOneDayAgo(){
    // given
    Instant beforeCall = Instant.now().minus(1, ChronoUnit.DAYS);

    // when
    userCleanupService.hardDeleteExpiredUsers();

    // then
    Instant afterCall = Instant.now().minus(1, ChronoUnit.DAYS);

    ArgumentCaptor<Instant> captor = ArgumentCaptor.forClass(Instant.class);
    verify(userRepository).deleteExpiredSoftDeletedUsers(captor.capture());

    Instant deleteBefore = captor.getValue();

    assertThat(deleteBefore).isBetween(beforeCall, afterCall);
  }
}