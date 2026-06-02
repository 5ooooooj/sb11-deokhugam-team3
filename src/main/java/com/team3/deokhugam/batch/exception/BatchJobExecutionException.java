package com.team3.deokhugam.batch.exception;

public class BatchJobExecutionException extends RuntimeException {
  public BatchJobExecutionException(String jobName) {
    super("[배치] Job 실행 실패: " + jobName);
  }
}
