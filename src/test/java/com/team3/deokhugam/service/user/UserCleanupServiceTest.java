package com.team3.deokhugam.service.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.team3.deokhugam.repository.user.UserRepository;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
    // given
    given(userRepository.deleteExpiredSoftDeletedUsers(any(Instant.class)))
        .willReturn(3);

    // when
    int result = userCleanupService.hardDeleteExpiredUsers();

    // then
    assertThat(result).isEqualTo(3);
    verify(userRepository).deleteExpiredSoftDeletedUsers(any(Instant.class));
  }
}