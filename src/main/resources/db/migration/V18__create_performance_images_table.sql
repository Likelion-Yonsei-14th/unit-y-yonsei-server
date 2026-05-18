CREATE TABLE performance_images
(
    id             BIGINT      NOT NULL AUTO_INCREMENT,
    performance_id BIGINT      NOT NULL,
    image_url      TEXT        NOT NULL,
    image_order    INT         NOT NULL,
    image_type     VARCHAR(20) NOT NULL,
    created_at     DATETIME(6) NOT NULL,
    updated_at     DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_performance_images_performance_order (performance_id, image_order, id),
    CONSTRAINT fk_performance_images_performance
        FOREIGN KEY (performance_id) REFERENCES performances (id)
)
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
