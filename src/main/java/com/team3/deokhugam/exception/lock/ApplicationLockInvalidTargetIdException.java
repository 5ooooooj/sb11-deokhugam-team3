package com.team3.deokhugam.exception.lock;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class ApplicationLockInvalidTargetIdException extends DeokhugamException {

  public ApplicationLockInvalidTargetIdException() {
    super(ErrorCode.APPLICATION_LOCK_INVALID_TARGET_ID);
  }
}