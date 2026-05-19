package com.buddkitv2.global.exception;

public class NotificationNotFoundException extends RuntimeException {
    public NotificationNotFoundException() {
        super("존재하지 않는 알림입니다.");
    }
}
