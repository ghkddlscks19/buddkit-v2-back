package com.buddkitv2.global.exception;

public class ClubLeaderCannotLeaveException extends RuntimeException {
    public ClubLeaderCannotLeaveException() {
        super("모임장은 탈퇴할 수 없습니다.");
    }
}
