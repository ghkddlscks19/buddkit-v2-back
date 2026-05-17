package com.buddkitv2.global.exception;

public class AlreadyLikedClubException extends RuntimeException {
    public AlreadyLikedClubException() {
        super("이미 관심 모임으로 등록된 모임입니다.");
    }
}
