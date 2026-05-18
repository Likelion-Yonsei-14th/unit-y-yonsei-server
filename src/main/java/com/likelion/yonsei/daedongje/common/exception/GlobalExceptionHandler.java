package com.likelion.yonsei.daedongje.common.exception;

import com.likelion.yonsei.daedongje.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
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
 *   <li>{@link MethodArgumentNotValidException} — Bean Validation 실패 (400 INVALID_INPUT, 필드/글로벌 에러 모두 포함)</li>
 *   <li>{@link HttpMessageNotReadableException} — 요청 본문 파싱 실패 (400 INVALID_INPUT)</li>
 *   <li>{@link MissingServletRequestParameterException} — 필수 파라미터 누락 (400 INVALID_INPUT)</li>
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
        // 필드 단위 에러 + 객체 단위(global) 제약 모두 포함하기 위해 getAllErrors 사용
        String details = e.getBindingResult().getAllErrors().stream()
                .map(err -> {
                    if (err instanceof FieldError fe) {
                        return fe.getField() + ": " + fe.getDefaultMessage();
                    }
                    return err.getDefaultMessage();
                })
                .collect(Collectors.joining(", "));

        ErrorCode code = CommonErrorCode.INVALID_INPUT;
        // details 가 비어있으면 (모든 에러의 message 가 null) 기본 메시지로 fallback
        String message = details.isBlank() ? code.getMessage() : details;

        log.warn("Validation failed: {}", message);
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleMessageNotReadable(HttpMessageNotReadableException e) {
        log.warn("Malformed request body: {}", e.getMessage());
        ErrorCode code = CommonErrorCode.INVALID_INPUT;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.error(code.getCode(), "요청 본문을 해석할 수 없습니다."));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParameter(MissingServletRequestParameterException e) {
        log.warn("Missing required parameter: {}", e.getParameterName());
        ErrorCode code = CommonErrorCode.INVALID_INPUT;
        return ResponseEntity
                .status(code.getStatus())
                .body(ApiResponse.error(code.getCode(),
                        "필수 파라미터가 누락되었습니다: " + e.getParameterName()));
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
