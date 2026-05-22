CREATE TABLE satisfaction_reviews
(
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    rating     INT         NOT NULL,
    content    VARCHAR(1000),
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
)
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
