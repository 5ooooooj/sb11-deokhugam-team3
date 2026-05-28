package com.team3.deokhugam.dto.notification;

import com.team3.deokhugam.domain.notification.NotificationType;
import java.time.Instant;
import java.util.UUID;

public record NotificationDto(
    UUID id,
    UUID userId,
    UUID reviewId,
    String reviewContent,
    String message,
    NotificationType type,
    boolean confirmed,
    Instant createdAt,
    Instant updatedAt
) {}