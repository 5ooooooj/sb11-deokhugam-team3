package com.team3.deokhugam.exception.book;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class InvalidBookSearchConditionException extends DeokhugamException {

  public InvalidBookSearchConditionException() {
    super(ErrorCode.INVALID_INPUT);
  }
}
