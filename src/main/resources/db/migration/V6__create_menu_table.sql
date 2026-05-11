CREATE TABLE menus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booth_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    price INT NOT NULL,
    image_url VARCHAR(255),
    is_sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT fk_menus_booth
        FOREIGN KEY (booth_id)
        REFERENCES booths(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;