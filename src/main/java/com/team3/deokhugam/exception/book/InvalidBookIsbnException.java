package com.team3.deokhugam.exception.book;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class InvalidBookIsbnException extends DeokhugamException {

  public InvalidBookIsbnException() {
    super(ErrorCode.INVALID_BOOK_ISBN);
  }
}
