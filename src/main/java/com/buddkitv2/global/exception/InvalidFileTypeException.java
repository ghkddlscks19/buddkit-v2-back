package com.buddkitv2.global.exception;

public class InvalidFileTypeException extends RuntimeException {
    public InvalidFileTypeException() {
        super("jpeg, png 형식의 이미지만 업로드 가능합니다.");
    }
}
