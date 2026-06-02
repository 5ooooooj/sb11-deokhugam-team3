package com.team3.deokhugam;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DeokhugamApplication {

  public static void main(String[] args) {
    SpringApplication.run(DeokhugamApplication.class, args);
  }

}
