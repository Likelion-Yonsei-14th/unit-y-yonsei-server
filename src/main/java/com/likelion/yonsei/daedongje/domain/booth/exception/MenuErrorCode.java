package com.likelion.yonsei.daedongje.domain.booth.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum MenuErrorCode implements ErrorCode {

    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "MENU_001", "존재하지 않는 메뉴입니다."),
    DUPLICATE_MENU_DISPLAY_ORDER(HttpStatus.CONFLICT, "MENU_002", "이미 사용 중인 메뉴 표시 순서입니다."),
    INVALID_MENU_REORDER_REQUEST(HttpStatus.BAD_REQUEST, "MENU_003", "메뉴 순서 목록이 부스의 메뉴 구성과 일치하지 않습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
