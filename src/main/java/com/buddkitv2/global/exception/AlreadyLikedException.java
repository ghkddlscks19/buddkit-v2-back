package com.buddkitv2.global.exception;

public class AlreadyLikedException extends RuntimeException {
    public AlreadyLikedException() {
        super("이미 좋아요한 피드입니다.");
    }
}
