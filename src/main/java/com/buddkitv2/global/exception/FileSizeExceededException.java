package com.buddkitv2.global.exception;

public class FileSizeExceededException extends RuntimeException {
    public FileSizeExceededException() {
        super("파일 크기는 5MB를 초과할 수 없습니다.");
    }
}
