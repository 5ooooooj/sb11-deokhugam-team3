package com.team3.deokhugam.exception.lock;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class ApplicationLockExpressionEvaluationException extends DeokhugamException {

  public ApplicationLockExpressionEvaluationException() {
    super(ErrorCode.APPLICATION_LOCK_EXPRESSION_EVALUATION_FAILED);
  }
}