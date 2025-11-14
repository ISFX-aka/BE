package com.isfx.shim.global.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    // 400 BAD_REQUEST: 잘못된 요청
    FILE_EMPTY(HttpStatus.BAD_REQUEST, "전송된 파일이 비어있습니다."),
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다."),
    RECORD_ALREADY_EXISTS(HttpStatus.CONFLICT, "오늘의 기록이 이미 존재합니다."),
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "요청한 기록을 찾을 수 없습니다."),
    RECORD_FORBIDDEN(HttpStatus.FORBIDDEN, "해당 기록에 접근 권한이 없습니다."),

    // [추가] 400 - 닉네임 유효성 검사 (API 명세)
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "잘못된 요청입니다. 'name' 값이 비어있거나 유효하지 않습니다."),
    // [추가] 400 - 통계 기간 유효성 검사 (API 명세)
    INVALID_PERIOD_REQUEST(HttpStatus.BAD_REQUEST, "잘못된 요청입니다. 'period' 값(week/month)이 누락되었거나 유효하지 않습니다."),

    // 404 NOT_FOUND: 리소스를 찾을 수 없음
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사용자를 찾을 수 없습니다."),

    // [추가] 409 CONFLICT: 리소스 충돌
    CONFLICT_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),

    // 500 INTERNAL_SERVER_ERROR: 서버 내부 오류
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드에 실패했습니다."),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String message;
}