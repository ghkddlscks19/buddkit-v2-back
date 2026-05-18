package com.buddkitv2.domain.notification.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"NOTIFICATION_TYPE\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class NotificationType {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "type_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    private NotificationTypeEnum type;

    @Column(columnDefinition = "text")
    private String template;

    public static NotificationType of(NotificationTypeEnum type, String template) {
        NotificationType nt = new NotificationType();
        nt.type = type;
        nt.template = template;
        return nt;
    }
}
