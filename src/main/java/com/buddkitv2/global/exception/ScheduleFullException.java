package com.buddkitv2.global.exception;

public class ScheduleFullException extends RuntimeException {
    public ScheduleFullException() {
        super("스케줄 정원이 가득 찼습니다.");
    }
}
