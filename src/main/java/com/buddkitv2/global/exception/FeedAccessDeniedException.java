package com.buddkitv2.global.exception;

public class FeedAccessDeniedException extends RuntimeException {
    public FeedAccessDeniedException() {
        super("피드에 대한 권한이 없습니다.");
    }
}
