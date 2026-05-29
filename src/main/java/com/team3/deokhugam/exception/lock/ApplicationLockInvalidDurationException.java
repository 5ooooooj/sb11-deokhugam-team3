package com.team3.deokhugam.exception.lock;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class ApplicationLockInvalidDurationException extends DeokhugamException {

  public ApplicationLockInvalidDurationException() {
    super(ErrorCode.APPLICATION_LOCK_INVALID_DURATION);
  }
}
