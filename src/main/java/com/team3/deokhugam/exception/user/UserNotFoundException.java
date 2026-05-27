package com.team3.deokhugam.exception.user;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class UserNotFoundException extends DeokhugamException {

  public UserNotFoundException() {
    super(ErrorCode.USER_NOT_FOUND);
  }
}
