package com.buddkitv2.global.exception;

public class AlreadyJoinedScheduleException extends RuntimeException {
    public AlreadyJoinedScheduleException() {
        super("이미 참여한 스케줄입니다.");
    }
}
