package com.buddkitv2.global.exception;

public class AlreadyRegisteredException extends RuntimeException {
    public AlreadyRegisteredException() {
        super("이미 가입된 회원입니다.");
    }
}
