CREATE TABLE lost_items (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(100) NOT NULL,
    description TEXT NULL,
    image_url VARCHAR(255) NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'STORED',
    found_location_id BIGINT NULL,
    storage_location_id BIGINT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
