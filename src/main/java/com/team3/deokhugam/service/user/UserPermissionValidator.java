package com.team3.deokhugam.service.user;

import com.team3.deokhugam.exception.user.UserForbiddenException;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserPermissionValidator {

  public void validateSelf(UUID targetUserId, UUID requestUserId) {
    if (!targetUserId.equals(requestUserId)) {
      throw new UserForbiddenException();
    }
  }
}
