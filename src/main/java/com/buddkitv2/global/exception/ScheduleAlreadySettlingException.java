package com.buddkitv2.global.exception;

public class ScheduleAlreadySettlingException extends RuntimeException {
    public ScheduleAlreadySettlingException() {
        super("이미 정산이 진행 중인 스케줄입니다.");
    }
}
