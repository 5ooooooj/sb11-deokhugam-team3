package com.team3.deokhugam.global.lock.aspect;

import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Component
@Aspect
public class ApplicationLockAspect {

  @Around("@annotation(ApplicationLock)")
  public void lock() {
  }

}
