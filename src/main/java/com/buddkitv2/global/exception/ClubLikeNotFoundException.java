package com.buddkitv2.global.exception;

public class ClubLikeNotFoundException extends RuntimeException {
    public ClubLikeNotFoundException() {
        super("관심 모임으로 등록되지 않은 모임입니다.");
    }
}
