package com.buddkitv2.global.exception;

public class ScheduleNotRecruitingException extends RuntimeException {
    public ScheduleNotRecruitingException() {
        super("모집 중인 스케줄이 아닙니다.");
    }
}
