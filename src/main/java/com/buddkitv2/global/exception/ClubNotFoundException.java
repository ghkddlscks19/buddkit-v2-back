package com.buddkitv2.global.exception;

public class ClubNotFoundException extends RuntimeException {
    public ClubNotFoundException() {
        super("존재하지 않는 모임입니다.");
    }
}
