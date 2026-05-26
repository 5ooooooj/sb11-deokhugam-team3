package com.team3.deokhugam.global.error;

import com.team3.deokhugam.exception.book.BookAlreadyExistsException;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
  @ExceptionHandler(BookAlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleBookAlreadyExistsException(BookAlreadyExistsException exception) {
    return new ErrorResponse(
        Instant.now(),
        "BOOK_ISBN_DUPLICATED",
        exception.getMessage(),
        Map.of(),
        HttpStatus.CONFLICT.value()
    );
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleMethodArgumentNotValidException(
      MethodArgumentNotValidException exception) {
    return new ErrorResponse(
        Instant.now(),
        "INVALID_REQUEST",
        "입력값 검증에 실패했습니다.",
        Map.of(),
        HttpStatus.BAD_REQUEST.value()
    );
  }
}
