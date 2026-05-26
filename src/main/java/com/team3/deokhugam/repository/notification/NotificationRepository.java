package com.team3.deokhugam.repository.notification;

import com.team3.deokhugam.domain.notification.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {
}