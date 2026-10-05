-- 합성 교통 관측 데이터(실제 프로젝트 데이터 아님). 31개 지역, 2023-01 ~ 2024-12(24개월).
-- 사용법: psql -v rows=20000000 -f sql/10_plain.sql
DROP TABLE IF EXISTS obs_plain CASCADE;
CREATE TABLE obs_plain (
  obs_time      timestamptz NOT NULL,
  region_id     smallint    NOT NULL,   -- 1..31
  road_id       integer     NOT NULL,   -- 1..2000
  direction     smallint    NOT NULL,   -- 0/1
  vehicle_class smallint    NOT NULL,   -- 1..6
  speed_kmh     real        NOT NULL,
  volume        integer     NOT NULL
);

SELECT setseed(0.42);   -- 재현 가능한 데이터

INSERT INTO obs_plain
SELECT TIMESTAMPTZ '2023-01-01 00:00:00+09' + random() * interval '731 days',
       (1 + floor(random() * 31))::smallint,
       (1 + floor(random() * 2000))::int,
       floor(random() * 2)::smallint,
       (1 + floor(random() * 6))::smallint,
       (20 + random() * 80)::real,
       floor(random() * 60)::int
FROM generate_series(1, :rows);

ANALYZE obs_plain;
