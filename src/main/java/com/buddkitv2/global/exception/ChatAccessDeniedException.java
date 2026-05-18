package com.buddkitv2.global.exception;

public class ChatAccessDeniedException extends RuntimeException {
    public ChatAccessDeniedException() {
        super("채팅방에 대한 권한이 없습니다.");
    }
}
