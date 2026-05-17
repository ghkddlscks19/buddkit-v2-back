package com.buddkitv2.global.exception;

public class ClubAccessDeniedException extends RuntimeException {
    public ClubAccessDeniedException() {
        super("모임장만 수행할 수 있는 작업입니다.");
    }
}
