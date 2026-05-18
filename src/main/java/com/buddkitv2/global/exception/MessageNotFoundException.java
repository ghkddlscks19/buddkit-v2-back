package com.buddkitv2.global.exception;

public class MessageNotFoundException extends RuntimeException {
    public MessageNotFoundException() {
        super("메시지를 찾을 수 없습니다.");
    }
}
