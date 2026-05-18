CREATE TABLE booth_click_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booth_id BIGINT NOT NULL,
    clicked_at DATETIME(6) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    INDEX idx_booth_click_logs_booth_id (booth_id),
    INDEX idx_booth_click_logs_clicked_at (clicked_at),

    CONSTRAINT fk_booth_click_logs_booth
        FOREIGN KEY (booth_id)
        REFERENCES booths(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
