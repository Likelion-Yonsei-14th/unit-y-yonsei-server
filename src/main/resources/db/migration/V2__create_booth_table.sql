-- 부스(Booth) 테이블 생성
-- admin_id, location_id 는 외래 키 제약 없이 숫자 컬럼만 추가
-- 참조 테이블(관리자, 지도 위치) 완성 후 별도 migration 으로 FK 제약 추가 예정

CREATE TABLE booths
(
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    admin_id       BIGINT        NOT NULL,
    name           VARCHAR(50)   NOT NULL,
    organization   VARCHAR(100)  NOT NULL,
    description    TEXT,
    date           INT           NOT NULL,
    open_time      TIME          NOT NULL,
    close_time     TIME          NOT NULL,
    sector         VARCHAR(10)   NOT NULL,
    location       INT           NOT NULL,
    status         VARCHAR(10)   NOT NULL,
    is_food        BOOLEAN       NOT NULL DEFAULT FALSE,
    instagram      TEXT,
    is_reservable  BOOLEAN       NOT NULL DEFAULT TRUE,
    account        TEXT,
    location_id    BIGINT,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci;
