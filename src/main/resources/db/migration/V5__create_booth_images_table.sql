-- 부스 이미지(BoothImage) 테이블 생성
-- booth_id 는 booths 테이블을 참조
-- created_at, updated_at 은 BaseEntity 대응 컬럼

CREATE TABLE booth_images
(
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    booth_id       BIGINT        NOT NULL,
    image_url      VARCHAR(255)  NOT NULL,
    display_order  INT           NOT NULL,
    created_at     DATETIME(6)   NOT NULL,
    updated_at     DATETIME(6)   NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_booth_images_booth
        FOREIGN KEY (booth_id) REFERENCES booths (id)
)
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;