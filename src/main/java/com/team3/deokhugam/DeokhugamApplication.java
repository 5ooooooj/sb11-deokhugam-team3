package com.team3.deokhugam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableRetry
@SpringBootApplication
@EnableScheduling
public class DeokhugamApplication {

  public static void main(String[] args) {
    SpringApplication.run(DeokhugamApplication.class, args);
  }

}
