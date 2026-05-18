-- 예약 조회용 비밀번호(PIN) 컬럼 추가
-- 선택 입력값이므로 nullable

ALTER TABLE reservations
    ADD COLUMN pin VARCHAR(4) NULL AFTER reservation_number;
