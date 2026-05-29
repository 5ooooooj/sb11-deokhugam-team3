package com.team3.deokhugam.exception.lock;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class ApplicationLockNotFoundException extends DeokhugamException {
  public ApplicationLockNotFoundException() {
    super(ErrorCode.APPLICATION_LOCK_NOT_FOUND);
  }
}
