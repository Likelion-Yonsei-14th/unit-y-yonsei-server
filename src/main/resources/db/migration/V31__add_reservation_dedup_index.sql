-- 예약 광클 멱등 조회용 인덱스
-- ReservationService.create 의 findRecentDuplicates (booth_id + phone_number + created_at) 조회를
-- range scan 으로 처리하기 위한 인덱스. 컬럼 추가 없음.
CREATE INDEX idx_reservations_dedup
    ON reservations (booth_id, phone_number, created_at);
