-- 조회 패턴에 맞춘 인덱스: 지역+시간, 도로+시간
-- 단일 테이블과 파티션 테이블(각 파티션에 자동 생성)에 같은 인덱스를 만든다.
CREATE INDEX idx_plain_region_time ON obs_plain (region_id, obs_time);
CREATE INDEX idx_plain_road_time   ON obs_plain (road_id,   obs_time);
CREATE INDEX idx_part_region_time  ON obs_part  (region_id, obs_time);
CREATE INDEX idx_part_road_time    ON obs_part  (road_id,   obs_time);
ANALYZE obs_plain;
ANALYZE obs_part;
