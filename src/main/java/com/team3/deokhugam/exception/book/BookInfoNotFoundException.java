package com.team3.deokhugam.exception.book;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class BookInfoNotFoundException extends DeokhugamException {

  public BookInfoNotFoundException() {
    super(ErrorCode.BOOK_INFO_NOT_FOUND);
  }
}
