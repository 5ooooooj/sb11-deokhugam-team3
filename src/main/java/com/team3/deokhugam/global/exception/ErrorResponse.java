package com.team3.deokhugam.global.exception;

import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ErrorResponse {

  private final LocalDateTime timestamp;

  private final int status;

  private final String message;

  private final String details;

  @Builder
  public ErrorResponse(int status, String message, String details) {
    this.timestamp = LocalDateTime.now();
    this.status = status;
    this.message = message;
    this.details = details;
  }
}