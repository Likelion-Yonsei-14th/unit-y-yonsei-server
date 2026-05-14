CREATE TABLE creators (
    id BIGINT NOT NULL AUTO_INCREMENT,
    part_name VARCHAR(100),
    department_name VARCHAR(100),
    student_id VARCHAR(20),
    name VARCHAR(50),
    display_order INT,
    PRIMARY KEY (id)
);
