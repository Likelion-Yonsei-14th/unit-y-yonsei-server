-- 예약(Reservation) 테이블 생성
-- booth_id 는 booths 테이블을 참조
-- reservation_number 는 부스별 순번 (booth_id + reservation_number 복합 유니크)
-- created_at, updated_at 은 BaseEntity 대응 컬럼

CREATE TABLE reservations
(
    id                 BIGINT      NOT NULL AUTO_INCREMENT,
    booth_id           BIGINT      NOT NULL,
    reservation_number INT         NOT NULL,
    booker_name        VARCHAR(20) NOT NULL,
    phone_number       VARCHAR(20) NOT NULL,
    party_size         INT         NOT NULL,
    pin                VARCHAR(4),
    status             VARCHAR(20) NOT NULL,
    cancel_reason      TEXT,
    created_at         DATETIME(6) NOT NULL,
    updated_at         DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_reservations_booth_number UNIQUE (booth_id, reservation_number),
    CONSTRAINT fk_reservations_booth FOREIGN KEY (booth_id) REFERENCES booths (id)
)
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
