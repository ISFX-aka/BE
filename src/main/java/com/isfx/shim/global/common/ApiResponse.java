package com.isfx.shim.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.isfx.shim.global.exception.ErrorCode;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL) // data 필드가 null일 경우, JSON 응답에서 제외
public class ApiResponse<T> {

    private final int status;
    private final String message;
    private final T data;

    // 1. [Error] CustomException을 이용한 에러 응답
    public ApiResponse(ErrorCode errorCode) {
        this.status = errorCode.getStatus().value();
        this.message = errorCode.getMessage();
        this.data = null;
    }

    // 2. [Success] 공통 성공 응답 (데이터 포함)
    public ApiResponse(int status, String message, T data) {
        this.status = status;
        this.message = message;
        this.data = data;
    }

    // --- Static Factory Methods ---

    // 1. Error (CustomException)
    public static ApiResponse<Object> error(ErrorCode errorCode) {
        return new ApiResponse<>(errorCode);
    }

    // 2. Success (Data만)
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "Success", data);
    }

    // 3. Success (Message만)
    public static ApiResponse<Object> success(String message) {
        return new ApiResponse<>(200, message, null);
    }

    // 4. Success (Created - 201)
    public static <T> ApiResponse<T> created(T data) {
        return new ApiResponse<>(201, "Created", data);
    }
}