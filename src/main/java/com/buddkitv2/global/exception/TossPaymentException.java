package com.buddkitv2.global.exception;

public class TossPaymentException extends RuntimeException {
    public TossPaymentException() {
        super("결제 처리에 실패했습니다.");
    }
}
