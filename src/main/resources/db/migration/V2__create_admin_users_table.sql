CREATE TABLE admin_users (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             login_id VARCHAR(50) NOT NULL,
                             password_hash VARCHAR(255) NOT NULL,
                             organization VARCHAR(100) NOT NULL,
                             role VARCHAR(20) NOT NULL,
                             representative_name VARCHAR(50) NOT NULL,
                             representative_phone VARCHAR(30) NOT NULL,
                             created_at DATETIME(6) NOT NULL,
                             updated_at DATETIME(6) NOT NULL,
                             status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                             last_login_at DATETIME(6),
                             memo VARCHAR(500),

                             CONSTRAINT uk_admin_users_login_id UNIQUE (login_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;