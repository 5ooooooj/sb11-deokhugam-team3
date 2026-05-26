package com.team3.deokhugam.exception.s3;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class S3UploadException extends DeokhugamException {
  public S3UploadException() {
    super(ErrorCode.S3_UPLOAD_FAILED);
  }
}
