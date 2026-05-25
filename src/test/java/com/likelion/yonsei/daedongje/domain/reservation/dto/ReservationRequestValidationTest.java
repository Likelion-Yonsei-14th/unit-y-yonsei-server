package com.likelion.yonsei.daedongje.domain.reservation.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 예약 생성/수정 DTO 검증 통일(R-02) 회귀 테스트.
 * 생성 DTO 에 공백 시작 방지(@Pattern)와 인원 수 상한(@Max)이 수정 DTO 와 동일하게 적용되는지 검증한다.
 */
class ReservationRequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    @Test
    void createRejectsBookerNameStartingWithWhitespace() {
        ReservationCreateRequest request =
                new ReservationCreateRequest(" 홍길동", "010-1234-5678", 2, null, true);
        assertThat(validator.validateProperty(request, "bookerName")).isNotEmpty();
    }

    @Test
    void createRejectsPhoneNumberStartingWithWhitespace() {
        ReservationCreateRequest request =
                new ReservationCreateRequest("홍길동", " 010-1234-5678", 2, null, true);
        assertThat(validator.validateProperty(request, "phoneNumber")).isNotEmpty();
    }

    @Test
    void createRejectsPartySizeAboveMax() {
        ReservationCreateRequest request =
                new ReservationCreateRequest("홍길동", "010-1234-5678", 101, null, true);
        assertThat(validator.validateProperty(request, "partySize")).isNotEmpty();
    }

    @Test
    void createAcceptsValidRequest() {
        ReservationCreateRequest request =
                new ReservationCreateRequest("홍길동", "010-1234-5678", 2, "1234", true);
        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void updateRejectsNewPartySizeAboveMax() {
        ReservationUpdateRequest request =
                new ReservationUpdateRequest("홍길동", "010-1234-5678", null, "김철수", "010-9876-5432", 101);
        assertThat(validator.validateProperty(request, "newPartySize")).isNotEmpty();
    }
}
