package com.buddkitv2.global.exception;

public class FeedCommentNotFoundException extends RuntimeException {
    public FeedCommentNotFoundException() {
        super("존재하지 않는 댓글입니다.");
    }
}
