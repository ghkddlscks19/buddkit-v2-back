package com.buddkitv2.global.exception;

public class NotJoinedClubException extends RuntimeException {
    public NotJoinedClubException() {
        super("가입하지 않은 모임입니다.");
    }
}
