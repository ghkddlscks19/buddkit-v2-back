package com.buddkitv2.domain.notification.dto.event;

import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEventPayload {
    private NotificationTypeEnum type;
    private Long targetUserId;
    private String content;
}
