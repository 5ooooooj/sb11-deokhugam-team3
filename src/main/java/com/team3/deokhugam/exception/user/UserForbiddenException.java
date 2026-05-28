package com.team3.deokhugam.exception.user;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class UserForbiddenException extends DeokhugamException {

  public UserForbiddenException() {
    super(ErrorCode.USER_FORBIDDEN);
  }
}
