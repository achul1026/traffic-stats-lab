#!/usr/bin/env bash
# API 실험용 데이터셋(기본 500만 건): 월 파티션 원본 + 일 단위 집계 테이블. 원본 단일 테이블은 만든 뒤 삭제한다.
set -euo pipefail
cd "$(dirname "$0")/.."
ROWS="${ROWS:-5000000}"
./scripts/pg.sh init
./scripts/pg.sh start || true
psql -h /tmp -p "${LAB_PGPORT:-55432}" -U lab -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname='lab'" | grep -q 1 \
  || psql -h /tmp -p "${LAB_PGPORT:-55432}" -U lab -d postgres -c "CREATE DATABASE lab"
./scripts/pg.sh psql -q -f sql/00_reset.sql
./scripts/pg.sh psql -q -v ON_ERROR_STOP=1 -v rows="$ROWS" -f sql/10_plain.sql
./scripts/pg.sh psql -q -v ON_ERROR_STOP=1 -f sql/20_partitioned.sql
./scripts/pg.sh psql -q -v ON_ERROR_STOP=1 -f sql/40_rollup.sql
./scripts/pg.sh psql -q -v ON_ERROR_STOP=1 -c "DROP TABLE obs_plain; CREATE INDEX idx_part_region_time ON obs_part (region_id, obs_time); CREATE INDEX idx_part_road_time ON obs_part (road_id, obs_time); ANALYZE obs_part;"
./scripts/pg.sh psql -Atc "CHECKPOINT" >/dev/null
./scripts/pg.sh psql -Atc "select count(*) from obs_part" -c "select count(*) from daily_region_stats"
