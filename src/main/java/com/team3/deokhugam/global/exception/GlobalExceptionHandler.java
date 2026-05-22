package com.team3.deokhugam.global.exception;

import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  // 400 Bad Request
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidationException(
      MethodArgumentNotValidException e
  ) {
    String details = e.getBindingResult().getFieldErrors().stream()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .collect(Collectors.joining(", "));

    ErrorResponse response = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message("잘못된 요청입니다.")
        .details(details)
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  // 409 Conflict
  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(
      IllegalStateException e
  ) {
    ErrorResponse response = ErrorResponse.builder()
        .status(HttpStatus.CONFLICT.value())
        .message(e.getMessage())
        .details(e.getMessage())
        .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  // 409 Conflict
  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException e
  ) {
    ErrorResponse response = ErrorResponse.builder()
        .status(HttpStatus.CONFLICT.value())
        .message("데이터 충돌이 발생했습니다.")
        .details("데이터 중복 제약 조건을 위반했습니다.")
        .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  // 500 Internal Server Error
  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleException(Exception e) {
    ErrorResponse response = ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .message("서버 내부 오류가 발생했습니다.")
        .details("관리자에게 문의해주세요.")
        .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}