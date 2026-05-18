package com.likelion.yonsei.daedongje.common.exception;

import org.springframework.http.HttpStatus;

/**
 * 도메인 에러 코드 enum 들이 공통으로 구현하는 인터페이스.
 *
 * <p>구현 예:
 * <pre>{@code
 *   public enum BoothErrorCode implements ErrorCode {
 *       BOOTH_NOT_FOUND(HttpStatus.NOT_FOUND, "B-001", "부스를 찾을 수 없습니다.");
 *       // ...
 *   }
 * }</pre>
 *
 * <p>{@code code} 는 {@code {도메인}-{번호}} 형식 ({@code B-001}, {@code R-002}, {@code COMMON-500} 등).
 * Function ID 라벨 체계({@code B-부스}, {@code R-예약} 등)와 동일한 prefix 를 사용한다.
 */
public interface ErrorCode {

    HttpStatus getStatus();

    String getCode();

    String getMessage();
}
