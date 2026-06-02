package com.team3.deokhugam.exception.review;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class ReviewAlreadyExistsException extends DeokhugamException {

  public ReviewAlreadyExistsException() {
    super(ErrorCode.REVIEW_ALREADY_EXISTS);
  }
}