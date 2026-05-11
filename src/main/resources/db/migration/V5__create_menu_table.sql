CREATE TABLE menus (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booth_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(255),
    price INT NOT NULL,
    image_url VARCHAR(255),
    is_sold_out BOOLEAN NOT NULL DEFAULT FALSE,
    display_order INT,

    CONSTRAINT fk_menus_booth
        FOREIGN KEY (booth_id)
        REFERENCES booths(id)
);