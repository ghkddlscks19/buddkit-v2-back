package com.buddkitv2.domain.notification;

import com.buddkitv2.domain.common.BaseEntity;
import com.buddkitv2.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "\"NOTIFICATION\"")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    // 렌더링된 메시지
    @Column(length = 255)
    private String content;

    private Boolean isRead;

    private Boolean fcmSent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "type_id", nullable = false)
    private NotificationType notificationType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public static Notification create(String content, NotificationType type, User user) {
        Notification n = new Notification();
        n.content = content;
        n.notificationType = type;
        n.user = user;
        n.isRead = false;
        n.fcmSent = false;
        return n;
    }
}
