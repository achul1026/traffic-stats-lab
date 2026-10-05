-- 반복 조회되는 통계는 미리 계산해 두는 일 단위 집계 테이블.
-- 평균은 (합계, 건수)로 저장해서 합산 후에도 정확한 평균을 낼 수 있게 한다.
DROP TABLE IF EXISTS daily_region_stats;
CREATE TABLE daily_region_stats AS
SELECT (obs_time AT TIME ZONE 'Asia/Seoul')::date AS day,
       region_id,
       sum(volume)::bigint        AS total_volume,
       sum(speed_kmh::float8)     AS sum_speed,
       count(*)::bigint           AS samples
FROM obs_plain
GROUP BY 1, 2;
ALTER TABLE daily_region_stats ADD PRIMARY KEY (region_id, day);
CREATE INDEX idx_rollup_day ON daily_region_stats (day);
ANALYZE daily_region_stats;
