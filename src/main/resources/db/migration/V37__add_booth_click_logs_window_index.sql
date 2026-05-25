-- 인기 부스 집계(GET /api/home/popular-booths)는 booth_click_logs 를 clicked_at 시간 윈도우로
-- 필터한 뒤 booth_id 로 GROUP BY/COUNT 한다. 클릭 로그는 부스 조회마다 INSERT 되어 누적되므로
-- 매 호출의 풀스캔+temporary+filesort 비용이 시간이 갈수록 커진다(부하 테스트: 클릭 30만 행 ~220ms).
--
-- (clicked_at, booth_id) 복합 커버링 인덱스로 ① clicked_at 윈도우 range 스캔 ② booth_id 까지
-- 인덱스에 포함돼 index-only 집계가 되도록 한다(같은 테스트에서 ~80ms 로 단축 확인).
--
-- booth_click_logs 는 쓰기 빈번 테이블이라 인덱스 개수를 늘리지 않는다: 기존 단일 (clicked_at)
-- 인덱스는 새 복합 인덱스의 prefix 라 중복이므로 교체(DROP 후 ADD)한다.
-- (booth_id) 단일 인덱스는 FK·boothId 삭제 경로에서 쓰이므로 유지한다.
ALTER TABLE booth_click_logs
    DROP INDEX idx_booth_click_logs_clicked_at,
    ADD INDEX idx_booth_click_logs_window (clicked_at, booth_id);
