package com.team3.deokhugam.exception.ocr;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class InvalidOcrImageException extends DeokhugamException {

  public InvalidOcrImageException() {
    super(ErrorCode.INVALID_OCR_IMAGE);
  }
}
