package com.isfx.shim.global.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 우리가 정의한 CustomException 처리
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException ex) {
        return ResponseEntity
                .status(ex.getErrorCode().getStatus())
                .body(new ErrorResponse(ex.getErrorCode()));
    }

    // (400) @Valid 유효성 검사 실패 (PUT /me)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult()
                .getAllErrors()
                .get(0)
                .getDefaultMessage();

        // ErrorCode.INVALID_NICKNAME의 메시지를 사용하거나, @NotBlank의 메시지를 그대로 사용
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(errorMessage));
    }

    // (400) @RequestParam 누락 (GET /me/status)
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParamException(MissingServletRequestParameterException ex) {
        if ("period".equals(ex.getParameterName())) {
            return ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse(ErrorCode.INVALID_PERIOD_REQUEST));
        }

        String message = String.format("필수 파라미터 '%s'가 누락되었습니다.", ex.getParameterName());
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse(message));
    }

    // (500) 처리되지 않은 모든 예외
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleServerException(Exception ex) {
        // (중요) 실제 운영 시에는 ex.getMessage() 대신 로깅 후,
        // ErrorCode.SERVER_ERROR의 공통 메시지를 반환해야 합니다.
        // log.error("Internal Server Error: ", ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorResponse(ErrorCode.SERVER_ERROR));
    }
}