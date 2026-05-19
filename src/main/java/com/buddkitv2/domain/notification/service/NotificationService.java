package com.buddkitv2.domain.notification.service;

import com.buddkitv2.domain.notification.dto.response.NotificationResponse;
import com.buddkitv2.domain.notification.entity.Notification;
import com.buddkitv2.domain.notification.repository.NotificationRepository;
import com.buddkitv2.global.exception.NotificationNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public List<NotificationResponse> getNotifications(Long userId, Long lastId, int size) {
        List<Notification> notifications = lastId == null
                ? notificationRepository.findByUserId(userId, PageRequest.of(0, size))
                : notificationRepository.findByUserIdAndLastId(userId, lastId, PageRequest.of(0, size));
        return notifications.stream().map(NotificationResponse::from).toList();
    }

    @Transactional
    public void markAsRead(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(it -> it.getUser().getId().equals(userId))
                .orElseThrow(NotificationNotFoundException::new);
        n.markAsRead();
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsRead(userId);
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification n = notificationRepository.findById(notificationId)
                .filter(it -> it.getUser().getId().equals(userId))
                .orElseThrow(NotificationNotFoundException::new);
        notificationRepository.delete(n);
    }
}
