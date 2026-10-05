-- 월 단위 RANGE 파티션 테이블(경계는 한국 시간 자정). obs_plain 의 데이터를 그대로 복사한다.
-- (처음에는 경계를 세션 시간대로 만들어서, 시간대가 UTC 인 Docker 환경에서 데이터 적재가 실패했다. 그래서 명시적으로 고정)
DROP TABLE IF EXISTS obs_part CASCADE;
CREATE TABLE obs_part (LIKE obs_plain) PARTITION BY RANGE (obs_time);

DO $$
DECLARE m date := DATE '2023-01-01';
BEGIN
  WHILE m < DATE '2025-01-01' LOOP
    EXECUTE format(
      'CREATE TABLE obs_part_%s PARTITION OF obs_part FOR VALUES FROM (%L) TO (%L)',
      to_char(m, 'YYYY_MM'),
      m::timestamp AT TIME ZONE 'Asia/Seoul',
      (m + INTERVAL '1 month')::date::timestamp AT TIME ZONE 'Asia/Seoul');   -- 세션 시간대와 무관하게 한국 시간 자정 경계
    m := (m + INTERVAL '1 month')::date;
  END LOOP;
END $$;

INSERT INTO obs_part SELECT * FROM obs_plain;
ANALYZE obs_part;
