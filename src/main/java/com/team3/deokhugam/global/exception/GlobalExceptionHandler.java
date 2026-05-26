package com.team3.deokhugam.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e
  ) {
    String details = e.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .orElse("요청값이 올바르지 않습니다.");

    ErrorResponse response = ErrorResponse.builder()
        .status(HttpStatus.BAD_REQUEST.value())
        .message("잘못된 요청입니다.")
        .details(details)
        .build();

    return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<ErrorResponse> handleIllegalStateException(
      IllegalStateException e
  ) {
    ErrorResponse response = ErrorResponse.builder()
        .status(HttpStatus.CONFLICT.value())
        .message("요청을 처리할 수 없습니다.")
        .details(e.getMessage())
        .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException e
  ) {
    log.warn("Data intergrity violation occurred: ", e);

    ErrorResponse response = ErrorResponse.builder()
        .status(HttpStatus.CONFLICT.value())
        .message("요청을 처리할 수 없습니다.")
        .details("데이터 제약 조건을 위반했습니다.")
        .build();

    return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
    log.error("Internal Server Error Occurred: ", e);

    ErrorResponse response = ErrorResponse.builder()
        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
        .message("서버 내부 오류가 발생했습니다.")
        .details("관리자에게 문의해주세요.")
        .build();

    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
  }
}