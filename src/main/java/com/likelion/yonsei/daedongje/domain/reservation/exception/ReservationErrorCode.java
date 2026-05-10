package com.likelion.yonsei.daedongje.domain.reservation.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum ReservationErrorCode implements ErrorCode {

    RESERVATION_NOT_FOUND(HttpStatus.NOT_FOUND, "R-001", "존재하지 않는 예약입니다."),
    BOOTH_NOT_RESERVABLE(HttpStatus.BAD_REQUEST, "R-002", "예약이 불가능한 부스입니다."),
    ALREADY_CANCELLED(HttpStatus.BAD_REQUEST, "R-003", "이미 취소된 예약입니다."),
    CANNOT_CONFIRM_CANCELLED(HttpStatus.BAD_REQUEST, "R-004", "취소된 예약은 입장 처리할 수 없습니다."),
    INVALID_STATUS_TRANSITION(HttpStatus.BAD_REQUEST, "R-005", "유효하지 않은 상태 변경입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    ReservationErrorCode(HttpStatus status, String code, String message) {
        this.status = status;
        this.code = code;
        this.message = message;
    }

    @Override
    public HttpStatus getStatus() { return status; }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}
