package com.likelion.yonsei.daedongje.domain.booth.exception;

import com.likelion.yonsei.daedongje.common.exception.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum BoothImageErrorCode implements ErrorCode {

    BOOTH_IMAGE_NOT_FOUND(HttpStatus.NOT_FOUND, "BOOTH_IMAGE_001", "존재하지 않는 부스 이미지입니다."),
    DUPLICATE_BOOTH_IMAGE_DISPLAY_ORDER(HttpStatus.CONFLICT, "BOOTH_IMAGE_002", "이미 사용 중인 부스 이미지 표시 순서입니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
