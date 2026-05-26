package com.team3.deokhugam.exception.global;

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

  @ExceptionHandler(DeokhugamException.class)
  public ResponseEntity<ErrorResponse> handleDeokhugamException(DeokhugamException e) {
    ErrorCode code = e.getErrorCode();
    log.warn("Business exception: {}", code);

    ErrorResponse response = ErrorResponse.builder()
        .code(code.name())
        .status(code.getStatus().value())
        .message(code.getMessage())
        .details(e.getMessage())
        .build();

    return ResponseEntity.status(code.getStatus()).body(response);
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
      MethodArgumentNotValidException e
  ) {
    log.warn("Validation failed: {}", e.getMessage());
    String details = e.getBindingResult().getFieldErrors().stream()
        .findFirst()
        .map(error -> error.getField() + ": " + error.getDefaultMessage())
        .orElse("요청값이 올바르지 않습니다.");

    ErrorResponse response = ErrorResponse.builder()
        .code(ErrorCode.INVALID_INPUT.name())
        .status(ErrorCode.INVALID_INPUT.getStatus().value())
        .message(ErrorCode.INVALID_INPUT.getMessage())
        .details(details)
        .build();

    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus()).body(response);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException e) {
    log.warn("Data integrity violation occurred: {}", e.getMessage());

    ErrorResponse response = ErrorResponse.builder()
        .code(ErrorCode.DATA_INTEGRITY_VIOLATION.name())
        .status(ErrorCode.DATA_INTEGRITY_VIOLATION.getStatus().value())
        .message(ErrorCode.DATA_INTEGRITY_VIOLATION.getMessage())
        .details("데이터 제약 조건을 위반했습니다.")
        .build();

    return ResponseEntity.status(ErrorCode.DATA_INTEGRITY_VIOLATION.getStatus()).body(response);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> handleGeneralException(Exception e) {
    log.error("Internal Server Error Occurred: ", e);

    ErrorResponse response = ErrorResponse.builder()
        .code(ErrorCode.INTERNAL_SERVER_ERROR.name())
        .status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus().value())
        .message(ErrorCode.INTERNAL_SERVER_ERROR.getMessage())
        .details("관리자에게 문의해주세요.")
        .build();

    return ResponseEntity.status(ErrorCode.INTERNAL_SERVER_ERROR.getStatus()).body(response);
  }
}