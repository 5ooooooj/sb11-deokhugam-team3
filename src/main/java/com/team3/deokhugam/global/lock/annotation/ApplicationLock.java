package com.team3.deokhugam.global.lock.annotation;

import com.team3.deokhugam.global.lock.domain.LockName;
import com.team3.deokhugam.global.lock.domain.LockTarget;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

// 메서드 전용 어노테이션
@Target(value = {ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApplicationLock {

  LockTarget target();

  String targetId();

  LockName lockName();

  String duration() default "3s";

}
