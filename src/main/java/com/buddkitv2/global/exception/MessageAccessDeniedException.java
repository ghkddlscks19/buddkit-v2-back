package com.buddkitv2.global.exception;

public class MessageAccessDeniedException extends RuntimeException {
    public MessageAccessDeniedException() {
        super("메시지를 삭제할 권한이 없습니다.");
    }
}
