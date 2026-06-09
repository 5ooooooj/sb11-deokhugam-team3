package com.team3.deokhugam.exception.comment;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class CommentForbiddenException extends DeokhugamException {
  public CommentForbiddenException() {
    super(ErrorCode.COMMENT_FORBIDDEN);
  }
}