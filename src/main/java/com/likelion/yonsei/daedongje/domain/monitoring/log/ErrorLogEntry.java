package com.likelion.yonsei.daedongje.domain.monitoring.log;

import java.time.LocalDateTime;

/**
 * 최근 ERROR 로그 1건. 링버퍼 저장 단위이자 관리자 API 응답 형태로 그대로 사용한다.
 * {@code throwable}은 예외가 있으면 "클래스명: 메시지" 형태, 없으면 null.
 */
public record ErrorLogEntry(
        LocalDateTime timestamp,
        String level,
        String logger,
        String message,
        String throwable
) {
}
