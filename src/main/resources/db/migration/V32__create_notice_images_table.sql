CREATE TABLE notice_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    notice_id BIGINT NOT NULL,
    image_url TEXT NOT NULL,
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uq_notice_images_notice_display_order
        UNIQUE (notice_id, display_order),
    CONSTRAINT fk_notice_images_notice
        FOREIGN KEY (notice_id) REFERENCES notice (id)
        ON DELETE CASCADE
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;

INSERT INTO notice_images (notice_id, image_url, display_order, created_at, updated_at)
SELECT id, image_url, 1, created_at, updated_at
FROM notice
WHERE image_url IS NOT NULL
  AND TRIM(image_url) <> '';

ALTER TABLE notice
    DROP COLUMN image_url;
