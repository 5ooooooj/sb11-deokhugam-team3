package com.team3.deokhugam.exception.global;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

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

  // 🔴 기존:
  // develop(HEAD) 쪽에는 필수 request parameter 누락 예외 처리가 추가되어 있었습니다.
  //
  // @ExceptionHandler(MissingServletRequestParameterException.class)
  // public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
  //     MissingServletRequestParameterException e) {
  //   log.warn("Missing request parameter: {}", e.getParameterName());
  //   return invalidInput(e.getParameterName() + " 파라미터가 필요합니다.");
  // }
  //
  // 🔵 변경:
  // develop의 변경사항이므로 유지합니다.
  @ExceptionHandler(MissingServletRequestParameterException.class)
  public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
      MissingServletRequestParameterException e
  ) {
    log.warn("Missing request parameter: {}", e.getParameterName());
    return invalidInput(e.getParameterName() + " 파라미터가 필요합니다.");
  }

  // 🔴 기존:
  // develop(HEAD) 쪽에는 필수 request header 누락 예외 처리가 추가되어 있었습니다.
  //
  // @ExceptionHandler(MissingRequestHeaderException.class)
  // public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
  //     MissingRequestHeaderException e) {
  //   log.warn("Missing request header: {}", e.getHeaderName());
  //   return invalidInput(e.getHeaderName() + " 헤더가 필요합니다.");
  // }
  //
  // 🔵 변경:
  // develop의 변경사항이므로 유지합니다.
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ErrorResponse> handleMissingRequestHeader(
      MissingRequestHeaderException e
  ) {
    log.warn("Missing request header: {}", e.getHeaderName());
    return invalidInput(e.getHeaderName() + " 헤더가 필요합니다.");
  }

  // 🔴 기존:
  // 충돌 전 develop(HEAD) 쪽과 #50 쪽 모두 MethodArgumentTypeMismatchException 처리를 가지고 있었지만,
  // 구현 방식과 details 메시지가 달라 충돌이 발생했습니다.
  //
  // develop(HEAD):
  // return invalidInput(e.getName() + " 값이 올바르지 않습니다.");
  //
  // #50:
  // String details = e.getName() + ": 요청값이 올바르지 않습니다.";
  // ErrorResponse response = ErrorResponse.builder() ... build();
  //
  // 🔵 변경:
  // #50에서 추가한 enum 바인딩 실패 응답 형식은 유지하되,
  // ErrorResponse 생성은 invalidInput 공통 메서드를 사용하도록 정리합니다.
  // orderBy, direction enum 바인딩 실패 시 400 INVALID_INPUT 응답을 반환합니다.
  @ExceptionHandler(MethodArgumentTypeMismatchException.class)
  public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
      MethodArgumentTypeMismatchException e
  ) {
    log.warn("Request parameter type mismatch: {}", e.getMessage());
    String details = e.getName() + ": 요청값이 올바르지 않습니다.";

    return invalidInput(details);
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ErrorResponse> handleDataIntegrityViolationException(
      DataIntegrityViolationException e
  ) {
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

  private ResponseEntity<ErrorResponse> invalidInput(String details) {
    ErrorResponse response = ErrorResponse.builder()
        .code(ErrorCode.INVALID_INPUT.name())
        .status(ErrorCode.INVALID_INPUT.getStatus().value())
        .message(ErrorCode.INVALID_INPUT.getMessage())
        .details(details)
        .build();

    return ResponseEntity.status(ErrorCode.INVALID_INPUT.getStatus()).body(response);
  }
}