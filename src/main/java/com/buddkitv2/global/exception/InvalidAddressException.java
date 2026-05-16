package com.buddkitv2.global.exception;

public class InvalidAddressException extends RuntimeException {
    public InvalidAddressException() {
        super("유효하지 않은 지역입니다.");
    }
}
