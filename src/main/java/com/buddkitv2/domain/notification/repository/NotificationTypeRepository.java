package com.buddkitv2.domain.notification.repository;

import com.buddkitv2.domain.notification.entity.NotificationType;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationTypeRepository extends JpaRepository<NotificationType, Long> {
    Optional<NotificationType> findByType(NotificationTypeEnum type);
}
