package com.buddkitv2.domain.notification.service;

import com.buddkitv2.domain.notification.dto.event.NotificationEventPayload;
import com.buddkitv2.domain.notification.entity.Notification;
import com.buddkitv2.domain.notification.entity.NotificationType;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import com.buddkitv2.domain.notification.repository.NotificationRepository;
import com.buddkitv2.domain.notification.repository.NotificationTypeRepository;
import com.buddkitv2.domain.user.entity.User;
import com.buddkitv2.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationEventConsumer {

    private final NotificationRepository notificationRepository;
    private final NotificationTypeRepository notificationTypeRepository;
    private final UserRepository userRepository;
    private final FcmService fcmService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "notification-events", groupId = "buddkit-notification")
    @Transactional
    public void handleNotificationEvent(String message) {
        try {
            NotificationEventPayload payload = objectMapper.readValue(message, NotificationEventPayload.class);
            User user = userRepository.findById(payload.getTargetUserId()).orElse(null);
            if (user == null) return;

            if (payload.getType() != NotificationTypeEnum.CHAT) {
                NotificationType type = notificationTypeRepository.findByType(payload.getType())
                        .orElseThrow();
                Notification notification = notificationRepository.save(
                        Notification.create(payload.getContent(), type, user));
                if (user.getFcmToken() != null) {
                    fcmService.send(user.getFcmToken(), payload.getContent());
                    notification.markFcmSent();
                }
            } else {
                if (user.getFcmToken() != null) {
                    fcmService.send(user.getFcmToken(), payload.getContent());
                }
            }
        } catch (Exception e) {
            log.error("[NotificationEventConsumer] 메시지 처리 실패: {}", e.getMessage(), e);
        }
    }
}
