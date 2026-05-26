package com.team3.deokhugam.exception.global;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

  // 공통
  INVALID_INPUT("잘못된 요청입니다.", HttpStatus.BAD_REQUEST),
  INTERNAL_SERVER_ERROR("서버 내부 오류가 발생했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  DATA_INTEGRITY_VIOLATION("데이터 제약 조건을 위반했습니다.", HttpStatus.CONFLICT),

  // Book
  BOOK_NOTFOUND("도서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  BOOK_ALREADY_EXISTS("이미 등록된 ISBN입니다.", HttpStatus.CONFLICT),

  // S3
  S3_UPLOAD_FAILED("S3 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  S3_DELETE_FAILED("S3 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  ;

  private final String message;
  private final HttpStatus status;
}
