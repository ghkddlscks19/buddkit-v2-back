package com.buddkitv2.global.exception;

public class InsufficientBalanceException extends RuntimeException {
    public InsufficientBalanceException() {
        super("포인트 잔액이 부족합니다.");
    }
}
