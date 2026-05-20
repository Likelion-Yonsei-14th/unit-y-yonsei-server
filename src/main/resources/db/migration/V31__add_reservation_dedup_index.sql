-- 예약 광클 멱등 조회용 인덱스
-- ReservationService.create 의 findRecentDuplicates 쿼리:
--   WHERE booth_id = ? AND phone_number = ? AND status = ? AND created_at >= ?
--   ORDER BY created_at DESC
-- equality(booth_id, phone_number, status) → range(created_at) 순서로 구성해
-- 인덱스만으로 필터·정렬을 모두 처리하도록 한다. 컬럼 추가 없음.
CREATE INDEX idx_reservations_dedup
    ON reservations (booth_id, phone_number, status, created_at);
