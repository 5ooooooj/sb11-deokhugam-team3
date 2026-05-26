package com.team3.deokhugam.global.exception;

public class LoginFailedException extends RuntimeException{

  public LoginFailedException() {
    super("이메일 또는 비밀번호가 불일치합니다.");
  }

}
