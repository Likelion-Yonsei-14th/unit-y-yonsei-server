CREATE TABLE performance_cheer_messages
(
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    performance_id BIGINT       NOT NULL,
    setlist_id     BIGINT,
    message        VARCHAR(255) NOT NULL,
    display_status VARCHAR(20)  NOT NULL DEFAULT 'VISIBLE',
    created_at     DATETIME(6)  NOT NULL,
    updated_at     DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    INDEX idx_performance_cheer_messages_performance_id (performance_id),
    INDEX idx_performance_cheer_messages_setlist_id (setlist_id),
    INDEX idx_performance_cheer_messages_created_at (created_at),
    INDEX idx_performance_cheer_messages_performance_created (performance_id, created_at, id),
    CONSTRAINT fk_performance_cheer_messages_performance
        FOREIGN KEY (performance_id) REFERENCES performances (id)
            ON DELETE CASCADE,
    CONSTRAINT fk_performance_cheer_messages_setlist
        FOREIGN KEY (setlist_id) REFERENCES performance_setlists (id)
            ON DELETE SET NULL
)
    DEFAULT CHARSET = utf8mb4
    COLLATE = utf8mb4_unicode_ci;
