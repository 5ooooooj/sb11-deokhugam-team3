package com.team3.deokhugam.exception.s3;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class S3DeleteException extends DeokhugamException {
  public S3DeleteException() {
    super(ErrorCode.S3_DELETE_FAILED);
  }
}
