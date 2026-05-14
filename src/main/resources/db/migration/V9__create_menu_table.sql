CREATE TABLE menus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booth_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    price INT NOT NULL,
    image_url TEXT,
    is_sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT,


    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,

    CONSTRAINT uk_menus_booth_display_order
        UNIQUE (booth_id, display_order),
        
    CONSTRAINT fk_menus_booth
        FOREIGN KEY (booth_id)
        REFERENCES booths(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
