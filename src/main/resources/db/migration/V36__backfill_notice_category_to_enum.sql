-- notice.category 는 V14 에서 VARCHAR(50) 자유 문자열로 도입돼 GENERAL/EVENT 등 임의 값이 저장돼 왔다.
-- 이제 엔티티가 @Enumerated(EnumType.STRING) 으로 읽으므로, enum(BLUERUN/BOOTH/PERFORMANCE/OTHERS)
-- 밖의 값이나 NULL 이 한 행이라도 있으면 findAll 조회 시 Hibernate 가 IllegalArgumentException 을
-- 던져 GET /api/notices 목록 전체가 500 이 된다.
-- enum 밖 값과 NULL 을 OTHERS 로 정규화해 기존 운영 데이터를 안전하게 변환한다.
-- 컬럼 nullable 과 생성 API 의 category optional 은 유지한다(요청/응답 계약 변경 없음).
UPDATE notice
SET category = 'OTHERS'
WHERE category IS NULL
   OR category NOT IN ('BLUERUN', 'BOOTH', 'PERFORMANCE', 'OTHERS');
