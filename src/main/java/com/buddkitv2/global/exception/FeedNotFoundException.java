package com.buddkitv2.global.exception;

public class FeedNotFoundException extends RuntimeException {
    public FeedNotFoundException() {
        super("존재하지 않는 피드입니다.");
    }
}
