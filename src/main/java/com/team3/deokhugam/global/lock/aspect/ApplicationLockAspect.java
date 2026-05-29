package com.team3.deokhugam.global.lock.aspect;

import com.team3.deokhugam.exception.lock.ApplicationLockExpressionEvaluationException;
import com.team3.deokhugam.exception.lock.ApplicationLockInvalidDurationException;
import com.team3.deokhugam.exception.lock.ApplicationLockInvalidTargetIdException;
import com.team3.deokhugam.global.lock.annotation.ApplicationLock;
import com.team3.deokhugam.global.lock.service.ApplicationLockService;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

@Component
@Aspect
@RequiredArgsConstructor
public class ApplicationLockAspect {

  private final ApplicationLockService applicationLockService;

  // SpEL 파싱
  private final ExpressionParser parser = new SpelExpressionParser();
  // 파라미터 이름 알아오기, 실행 시점에 계산
  private final DefaultParameterNameDiscoverer parameterNameDiscoverer =
      new DefaultParameterNameDiscoverer();

  @Around("@annotation(applicationLock)")
  public Object lock(
      ProceedingJoinPoint joinPoint,
      ApplicationLock applicationLock
      ) throws Throwable{
    String targetId = evaluateTargetId(joinPoint, applicationLock.targetId());
    Duration duration = parseDuration(applicationLock.duration());

    UUID lockId = applicationLockService.acquire(
        applicationLock.target(),
        targetId,
        applicationLock.lockName(),
        duration
    );

    try {
      return joinPoint.proceed();
    } finally {
      applicationLockService.release(lockId);
    }
  }

  private String evaluateTargetId(
      ProceedingJoinPoint joinPoint,
      String expression
  ) {
    try {
      MethodSignature signature = (MethodSignature) joinPoint.getSignature();

      String[] parameterNames =
          parameterNameDiscoverer.getParameterNames(signature.getMethod());

      if (parameterNames == null) {
        throw new ApplicationLockExpressionEvaluationException();
      }

      Object[] args = joinPoint.getArgs();
      StandardEvaluationContext context = new StandardEvaluationContext();

      for (int i = 0; i < parameterNames.length; i++) {
        context.setVariable(parameterNames[i], args[i]);
      }

      Object value = parser.parseExpression(expression).getValue(context);

      if (value == null || value.toString().isBlank()) {
        throw new ApplicationLockInvalidTargetIdException();
      }

      return value.toString();
    } catch (ApplicationLockInvalidTargetIdException
             | ApplicationLockExpressionEvaluationException e) {
      throw e;
    } catch (RuntimeException e) {
      throw new ApplicationLockExpressionEvaluationException();
    }
  }

  private Duration parseDuration(String value) {
    if (value == null || value.isBlank()) {
      throw new ApplicationLockInvalidDurationException();
    }

    String trimmed = value.trim().toLowerCase();

    if (trimmed.endsWith("ms")) {
      return Duration.ofMillis(parseNumber(trimmed, "ms"));
    }

    if (trimmed.endsWith("s")) {
      return Duration.ofSeconds(parseNumber(trimmed, "s"));
    }

    if (trimmed.endsWith("m")) {
      return Duration.ofMinutes(parseNumber(trimmed, "m"));
    }

    if (trimmed.endsWith("h")) {
      return Duration.ofHours(parseNumber(trimmed, "h"));
    }

    throw new ApplicationLockInvalidDurationException();
  }

  private long parseNumber(String value, String unit) {
    String number = value.replace(unit, "");

    try {
      long parsed = Long.parseLong(number);
      if (parsed <= 0) {
        throw new ApplicationLockInvalidDurationException();
      }
      return parsed;
    } catch (NumberFormatException e) {
      throw new ApplicationLockInvalidDurationException();
    }
  }
}
