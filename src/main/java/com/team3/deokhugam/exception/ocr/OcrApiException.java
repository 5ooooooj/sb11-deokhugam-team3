package com.team3.deokhugam.exception.ocr;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class OcrApiException extends DeokhugamException {

  public OcrApiException() {
    super(ErrorCode.OCR_API_FAILED);
  }
}
