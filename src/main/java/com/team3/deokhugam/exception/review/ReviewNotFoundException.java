package com.team3.deokhugam.exception.review;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class ReviewNotFoundException extends DeokhugamException {

  public ReviewNotFoundException() {
    super(ErrorCode.REVIEW_NOT_FOUND);
  }
}