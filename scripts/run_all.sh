#!/usr/bin/env bash
# 전체 재현: 서버 준비 → 데이터 생성 → 인덱스 없는 상태 측정(A,B) → 인덱스 생성 → 측정(C,D,E) → 검증 → 차트
# 사용법: ROWS=30000000 ./scripts/run_all.sh        (디스크 약 10GB 필요, 약 15분)
set -euo pipefail
cd "$(dirname "$0")/.."
ROWS="${ROWS:-30000000}"
./scripts/pg.sh init
./scripts/pg.sh start || true
psql -h /tmp -p "${LAB_PGPORT:-55432}" -U lab -d postgres -Atc "SELECT 1 FROM pg_database WHERE datname='lab'" | grep -q 1 \
  || psql -h /tmp -p "${LAB_PGPORT:-55432}" -U lab -d postgres -c "CREATE DATABASE lab"
./scripts/pg.sh psql -q -f sql/00_reset.sql
./scripts/pg.sh psql -q -v ON_ERROR_STOP=1 -v rows="$ROWS" -f sql/10_plain.sql
./scripts/pg.sh psql -q -v ON_ERROR_STOP=1 -f sql/20_partitioned.sql
./scripts/pg.sh psql -q -v ON_ERROR_STOP=1 -f sql/40_rollup.sql
rm -f results/timings.csv
python3 bench/run.py --variants A,B --runs 5
./scripts/pg.sh psql -q -v ON_ERROR_STOP=1 -f sql/30_indexes.sql
./scripts/pg.sh psql -Atc "CHECKPOINT" >/dev/null
python3 bench/run.py --variants C,D,E --runs 5
python3 bench/verify.py
python3 bench/chart.py
