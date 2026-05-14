CREATE TABLE creators (
    id BIGINT NOT NULL AUTO_INCREMENT,
    part_name VARCHAR(100),
    department_name VARCHAR(100),
    student_id VARCHAR(20),
    name VARCHAR(50),
    display_order INT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_unicode_ci;
