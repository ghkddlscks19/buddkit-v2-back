package com.buddkitv2.global.exception;

public class SettlementNotFoundException extends RuntimeException {
    public SettlementNotFoundException() {
        super("존재하지 않는 정산입니다.");
    }
}
