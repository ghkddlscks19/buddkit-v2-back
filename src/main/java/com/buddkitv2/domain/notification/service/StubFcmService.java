package com.buddkitv2.domain.notification.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StubFcmService implements FcmService {

    @Override
    public void send(String token, String body) {
        log.info("[FCM stub] token={}, body={}", token, body);
    }
}
