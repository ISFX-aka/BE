package com.isfx.shim.global.exception;

import lombok.Getter;

@Getter
public class ErrorResponse {
    private final String message;

    public ErrorResponse(String message) {
        this.message = message;
    }

    public ErrorResponse(ErrorCode errorCode) {
        this.message = errorCode.getMessage();
    }
}