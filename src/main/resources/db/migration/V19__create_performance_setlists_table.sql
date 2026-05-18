CREATE TABLE performance_setlists
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    performance_id BIGINT       NOT NULL,
    song_title     VARCHAR(100) NOT NULL,
    singer_name    VARCHAR(100) NOT NULL,
    song_order     INT          NOT NULL,
    note           VARCHAR(255),
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_performance_setlists_performance_order (performance_id, song_order, id),
    CONSTRAINT fk_performance_setlists_performance
        FOREIGN KEY (performance_id) REFERENCES performances (id)
)
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
