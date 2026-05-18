CREATE TABLE notice (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    image_url VARCHAR(255) NULL,
    is_pinned BOOLEAN NOT NULL DEFAULT FALSE,
    category VARCHAR(50) NULL,
    view_count INT NOT NULL DEFAULT 0,
    performance_id BIGINT NULL,
    booth_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
