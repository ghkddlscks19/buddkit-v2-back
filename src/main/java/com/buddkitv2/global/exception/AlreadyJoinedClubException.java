package com.buddkitv2.global.exception;

public class AlreadyJoinedClubException extends RuntimeException {
    public AlreadyJoinedClubException() {
        super("이미 가입한 모임입니다.");
    }
}
