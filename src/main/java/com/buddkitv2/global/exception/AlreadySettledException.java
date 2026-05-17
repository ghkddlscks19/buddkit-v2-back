package com.buddkitv2.global.exception;

public class AlreadySettledException extends RuntimeException {
    public AlreadySettledException() {
        super("이미 정산이 완료된 항목입니다.");
    }
}
