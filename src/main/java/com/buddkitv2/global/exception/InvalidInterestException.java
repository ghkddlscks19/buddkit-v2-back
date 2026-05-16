package com.buddkitv2.global.exception;

public class InvalidInterestException extends RuntimeException {
    public InvalidInterestException() {
        super("유효하지 않은 관심사가 포함되어 있습니다.");
    }
}
