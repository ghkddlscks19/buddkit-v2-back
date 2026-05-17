package com.buddkitv2.global.exception;

public class NotLikedException extends RuntimeException {
    public NotLikedException() {
        super("좋아요하지 않은 피드입니다.");
    }
}
