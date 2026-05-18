package com.buddkitv2.domain.notification.service;

import com.buddkitv2.domain.notification.entity.NotificationType;
import com.buddkitv2.domain.notification.entity.NotificationTypeEnum;
import com.buddkitv2.domain.notification.repository.NotificationTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class NotificationTypeSeeder implements CommandLineRunner {

    private final NotificationTypeRepository notificationTypeRepository;

    @Override
    @Transactional
    public void run(String... args) {
        seed(NotificationTypeEnum.SETTLEMENT, "{actor}님이 정산을 요청했습니다.");
        seed(NotificationTypeEnum.SCHEDULE, "{clubName} 모임에 새 정모가 생겼습니다.");
        seed(NotificationTypeEnum.LIKE, "{actor}님이 회원님의 게시물을 좋아합니다.");
        seed(NotificationTypeEnum.COMMENT, "{actor}님이 댓글을 남겼습니다.");
        seed(NotificationTypeEnum.CHAT, "");
    }

    private void seed(NotificationTypeEnum type, String template) {
        if (notificationTypeRepository.findByType(type).isEmpty()) {
            notificationTypeRepository.save(NotificationType.of(type, template));
        }
    }
}
