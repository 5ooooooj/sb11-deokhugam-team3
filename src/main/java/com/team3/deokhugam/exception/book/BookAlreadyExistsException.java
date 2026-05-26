package com.team3.deokhugam.exception.book;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class BookAlreadyExistsException extends DeokhugamException {

  public BookAlreadyExistsException() {
    super(ErrorCode.BOOK_ALREADY_EXISTS);
  }
}
