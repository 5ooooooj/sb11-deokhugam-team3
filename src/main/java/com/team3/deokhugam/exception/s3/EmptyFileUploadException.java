package com.team3.deokhugam.exception.s3;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class EmptyFileUploadException extends DeokhugamException {
  public EmptyFileUploadException() {
    super(ErrorCode.EMPTY_FILE_UPLOAD);
  }
}
