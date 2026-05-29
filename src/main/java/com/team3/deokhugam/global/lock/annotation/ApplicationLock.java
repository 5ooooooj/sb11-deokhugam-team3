package com.team3.deokhugam.global.lock.annotation;

import com.team3.deokhugam.global.lock.domain.LockName;
import com.team3.deokhugam.global.lock.domain.LockTarget;

public @interface ApplicationLock {

  LockTarget target();

  String targetId();

  LockName lockName();

  String duration() default "3s";

}
