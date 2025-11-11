package com.isfx.shim.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 400 Bad Request
    INVALID_PERIOD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다. 'period' 값(week/month)이 누락되었거나 유효하지 않습니다."),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "잘못된 요청입니다. 'name' 값이 비어있거나 유효하지 않습니다."),

    // 401 Unauthorized
    UNAUTHORIZED_USER(HttpStatus.UNAUTHORIZED, "인증되지 않은 사용자입니다. 요청을 처리하려면 로그인 후 시도해주세요."),

    // 404 Not Found
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다."), // (사용자를 찾을 수 없음)

    // 409 Conflict
    CONFLICT_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    // 500 Internal Server Error
    SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버에서 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}