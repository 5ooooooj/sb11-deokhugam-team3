package com.team3.deokhugam.exception.s3;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class InvalidRequestException extends DeokhugamException {
  public InvalidRequestException() {
    super(ErrorCode.INVALID_FILE_KEY);
  }
}
