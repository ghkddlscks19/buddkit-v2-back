package com.buddkitv2.global.exception;

public class ScheduleAccessDeniedException extends RuntimeException {
    public ScheduleAccessDeniedException() {
        super("스케줄에 대한 권한이 없습니다.");
    }
}
