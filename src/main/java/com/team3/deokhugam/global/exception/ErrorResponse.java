package com.team3.deokhugam.global.exception;

import java.time.Instant;
import lombok.Builder;
import lombok.Getter;

@Getter
public class ErrorResponse {

  private final Instant timestamp;

  private final int status;

  private final String message;

  private final String details;

  @Builder
  public ErrorResponse(int status, String message, String details) {
    this.timestamp = Instant.now();
    this.status = status;
    this.message = message;
    this.details = details;
  }
}