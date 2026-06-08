package com.team3.deokhugam.exception.naver;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class NaverApiException extends DeokhugamException {

  public NaverApiException() {
    super(ErrorCode.NAVER_API_FAILED);
  }
}
