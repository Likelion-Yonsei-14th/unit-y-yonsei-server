package com.likelion.yonsei.daedongje.domain.booth.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import org.springframework.http.HttpStatus;

public enum BoothErrorCode implements ErrorCode {

    BOOTH_NOT_FOUND(HttpStatus.NOT_FOUND, "B-001", "존재하지 않는 부스입니다."),
    INVALID_BOOTH_TIME(HttpStatus.BAD_REQUEST, "B-003", "운영 종료 시간은 시작 시간보다 늦어야 합니다."),
    DUPLICATE_BOOTH_NAME(HttpStatus.CONFLICT, "B-004", "이미 존재하는 부스 이름입니다."),
    BOOTH_CLICK_RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "B-005", "부스 클릭 요청이 너무 많습니다. 잠시 후 다시 시도해주세요."),
    BOOTH_HAS_RESERVATIONS(HttpStatus.CONFLICT, "B-006", "예약이 있어 부스를 삭제할 수 없습니다. 먼저 예약을 정리해주세요."),
    BOOTH_HAS_MENUS(HttpStatus.CONFLICT, "B-007", "메뉴가 있어 부스를 삭제할 수 없습니다. 먼저 메뉴를 정리해주세요."),
    BOOTH_HAS_NOTICES(HttpStatus.CONFLICT, "B-008", "공지가 있어 부스를 삭제할 수 없습니다. 먼저 공지를 정리해주세요."),
    DUPLICATE_BOOTH_ADMIN(HttpStatus.CONFLICT, "B-009", "이미 부스가 배정된 어드민입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;

    BoothErrorCode(HttpStatus status, String code, String message) {
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
