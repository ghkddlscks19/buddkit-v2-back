package com.buddkitv2.global.exception;

public class WalletNotFoundException extends RuntimeException {
    public WalletNotFoundException() {
        super("지갑 정보를 찾을 수 없습니다.");
    }
}
