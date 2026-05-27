package com.team3.deokhugam.exception.user;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class LoginFailedException extends DeokhugamException {

  public LoginFailedException() {
    super(ErrorCode.LOGIN_FAILED);
  }
}