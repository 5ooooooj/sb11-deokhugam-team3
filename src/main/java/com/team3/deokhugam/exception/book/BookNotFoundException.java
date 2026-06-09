package com.team3.deokhugam.exception.book;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class BookNotFoundException extends DeokhugamException {

  public BookNotFoundException() {
    super(ErrorCode.BOOK_NOT_FOUND);
  }
}
