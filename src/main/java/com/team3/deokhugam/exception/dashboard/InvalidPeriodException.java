package com.team3.deokhugam.exception.dashboard;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class InvalidPeriodException extends DeokhugamException {

  public InvalidPeriodException() {
    super(ErrorCode.INVALID_PERIOD);
  }
}
