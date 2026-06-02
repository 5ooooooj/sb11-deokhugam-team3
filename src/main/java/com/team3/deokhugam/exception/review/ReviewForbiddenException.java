package com.team3.deokhugam.exception.review;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class ReviewForbiddenException extends DeokhugamException {

  public ReviewForbiddenException() {
    super(ErrorCode.REVIEW_FORBIDDEN);
  }
}