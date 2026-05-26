package com.team3.deokhugam.exception.book;

public class BookAlreadyExistsException extends IllegalStateException {

  public BookAlreadyExistsException(String isbn) {
    super("이미 등록된 ISBN입니다. isbn=" + isbn);
  }
}
