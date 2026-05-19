package com.buddkitv2.domain.notification.service;

public interface FcmService {
    void send(String token, String body);
}
