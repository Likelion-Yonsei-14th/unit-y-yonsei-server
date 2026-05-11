-- PIN을 BCrypt 해시로 저장하도록 컬럼 길이 확장 (4 → 60)
ALTER TABLE reservations
    MODIFY COLUMN pin VARCHAR(60) NULL;