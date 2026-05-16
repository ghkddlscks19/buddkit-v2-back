package com.buddkitv2.global.exception;

public class TempTokenExpiredException extends RuntimeException {
    public TempTokenExpiredException() {
        super("만료되었거나 유효하지 않은 임시 토큰입니다.");
    }
}
