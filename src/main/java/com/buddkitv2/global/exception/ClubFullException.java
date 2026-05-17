package com.buddkitv2.global.exception;

public class ClubFullException extends RuntimeException {
    public ClubFullException() {
        super("모임 정원이 가득 찼습니다.");
    }
}
