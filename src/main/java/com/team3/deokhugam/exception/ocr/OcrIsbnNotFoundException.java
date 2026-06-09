package com.team3.deokhugam.exception.ocr;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class OcrIsbnNotFoundException extends DeokhugamException {

  public OcrIsbnNotFoundException() {
    super(ErrorCode.OCR_ISBN_NOT_FOUND);
  }
}
