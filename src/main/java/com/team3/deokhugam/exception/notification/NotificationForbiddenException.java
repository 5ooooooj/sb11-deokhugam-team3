package com.team3.deokhugam.exception.notification;

import com.team3.deokhugam.exception.global.DeokhugamException;
import com.team3.deokhugam.exception.global.ErrorCode;

public class NotificationForbiddenException extends DeokhugamException {
  public NotificationForbiddenException() {
    super(ErrorCode.NOTIFICATION_FORBIDDEN);
  }
}