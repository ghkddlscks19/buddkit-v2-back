package com.buddkitv2.global.exception;

public class ScheduleNotFoundException extends RuntimeException {
    public ScheduleNotFoundException() {
        super("존재하지 않는 스케줄입니다.");
    }
}
