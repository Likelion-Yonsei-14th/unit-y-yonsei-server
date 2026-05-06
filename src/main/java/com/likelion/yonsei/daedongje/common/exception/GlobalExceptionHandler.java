package com.likelion.yonsei.daedongje.common.exception;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 전역 예외 처리기.
 *
 * <p>모든 컨트롤러에서 발생한 예외를 {@link ApiResponse} 형태의 일관된 에러 응답으로 변환한다.
 * 처리 우선순위:
 * <ol>
 *   <li>{@link BusinessException} — 도메인 예외, 자체 보유한 {@link ErrorCode} 의 status/code 사용</li>
 *   <li>{@link MethodArgumentNotValidException} — Bean Validation 실패 (400 INVALID_INPUT)</li>
 *   <li>{@link HttpRequestMethodNotSupportedException} — 잘못된 HTTP 메서드 (405)</li>
 *   <li>{@link Exception} (catch-all) — 예상치 못한 예외, 500 INTERNAL_ERROR + 에러 로그</li>
 * </ol>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("BusinessException: code={}, message={}", errorCode.getCode(), e.getMessage());
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(ApiResponse.error(errorCode.getCode(), e.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException e) {
        String details = e.getBindingResult().getFieldErrors().stream()
                .map(err -> err.getField() + ": " + err.getDefaultMessage())
                .collect(Collectors.joining(", "));
        log.warn("Validation failed: {}", details);
        ErrorCode code = CommonErrorCode.INVALID_INPUT;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), details));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiResponse<Void>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        log.warn("Method not supported: {}", e.getMessage());
        ErrorCode code = CommonErrorCode.METHOD_NOT_ALLOWED;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), code.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception e) {
        log.error("Unexpected exception", e);
        ErrorCode code = CommonErrorCode.INTERNAL_ERROR;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), code.getMessage()));
    }
}
