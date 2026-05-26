package com.team3.deokhugam.exception.comment;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class CommentNotFoundException extends DeokhugamException {
  public CommentNotFoundException() {
    super(ErrorCode.COMMENT_NOT_FOUND);
  }
}