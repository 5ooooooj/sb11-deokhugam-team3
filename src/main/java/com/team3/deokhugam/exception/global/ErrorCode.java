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

  // User
  EMAIL_ALREADY_EXISTS("이미 사용중인 이메일입니다.", HttpStatus.CONFLICT),
  LOGIN_FAILED("로그인에 실패했습니다.", HttpStatus.UNAUTHORIZED),
  USER_NOT_FOUND("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),

  // Book
  BOOK_NOTFOUND("도서를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  BOOK_ALREADY_EXISTS("이미 등록된 ISBN입니다.", HttpStatus.CONFLICT),

  // Comment
  COMMENT_NOT_FOUND("댓글을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  COMMENT_FORBIDDEN("본인 댓글만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN),

  // Notification
  NOTIFICATION_NOT_FOUND("알림을 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  NOTIFICATION_FORBIDDEN("본인 알림만 읽음 처리할 수 있습니다.", HttpStatus.FORBIDDEN),

  // Review
  REVIEW_NOT_FOUND("리뷰를 찾을 수 없습니다.", HttpStatus.NOT_FOUND),
  REVIEW_FORBIDDEN("본인 리뷰만 수정/삭제할 수 있습니다.", HttpStatus.FORBIDDEN),
  REVIEW_ALREADY_EXISTS("이미 작성된 리뷰가 있습니다.", HttpStatus.CONFLICT),

  // S3
  S3_UPLOAD_FAILED("S3 업로드에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  S3_DELETE_FAILED("S3 삭제에 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
  ;

  private final String message;
  private final HttpStatus status;
}
