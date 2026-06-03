package com.team3.deokhugam.exception.book;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class BookForbiddenException extends DeokhugamException {

  public BookForbiddenException() {
    super(ErrorCode.BOOK_FORBIDDEN);
  }
}
