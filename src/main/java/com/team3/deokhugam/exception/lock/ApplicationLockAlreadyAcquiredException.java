package com.team3.deokhugam.exception.lock;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class ApplicationLockAlreadyAcquiredException extends DeokhugamException {
  public ApplicationLockAlreadyAcquiredException() {
    super(ErrorCode.APPLICATION_LOCK_ALREADY_ACQUIRED);
  }
}
