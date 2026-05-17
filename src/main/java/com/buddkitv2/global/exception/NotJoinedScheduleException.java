package com.buddkitv2.global.exception;

public class NotJoinedScheduleException extends RuntimeException {
    public NotJoinedScheduleException() {
        super("참여하지 않은 스케줄입니다.");
    }
}
