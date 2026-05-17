package com.buddkitv2.global.exception;

public class WithdrawnUserException extends RuntimeException {
    public WithdrawnUserException() {
        super("탈퇴한 회원입니다.");
    }
}
