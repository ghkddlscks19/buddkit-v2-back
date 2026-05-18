package com.buddkitv2.global.exception;

public class InvalidSearchConditionException extends RuntimeException {
    public InvalidSearchConditionException() {
        super("잘못된 검색 조건입니다. district는 city와 함께 사용해야 합니다.");
    }
}
