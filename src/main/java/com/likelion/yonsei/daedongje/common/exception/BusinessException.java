package com.likelion.yonsei.daedongje.common.exception;

/**
 * 비즈니스 규칙 위반·도메인 예외의 베이스 클래스.
 *
 * <p>모든 도메인 예외는 본 클래스를 상속하거나 직접 사용한다.
 * {@link GlobalExceptionHandler} 가 본 클래스를 잡아서 {@link ErrorCode} 의
 * {@code status} 와 {@code code} 로 응답을 구성한다.
 *
 * <p>사용 예:
 * <pre>{@code
 *   throw new BusinessException(BoothErrorCode.BOOTH_NOT_FOUND);
 *   throw new BusinessException(BoothErrorCode.BOOTH_CLOSED, "오후 6시 이후엔 접수가 불가합니다.");
 * }</pre>
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String overrideMessage) {
        super(overrideMessage);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
