package com.buddkitv2.domain.notification.dto.response;

import com.buddkitv2.domain.notification.entity.Notification;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class NotificationResponse {
    private Long notificationId;
    private String type;
    private String content;
    private Boolean isRead;
    private LocalDateTime createdAt;

    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(
                n.getId(),
                n.getNotificationType().getType().name(),
                n.getContent(),
                n.getIsRead(),
                n.getCreatedAt()
        );
    }
}
