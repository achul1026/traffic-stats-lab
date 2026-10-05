#!/usr/bin/env bash
# 프로젝트 폴더 안에 격리된 PostgreSQL 클러스터를 만들고 시작/중지한다. (시스템 설정은 건드리지 않음)
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PGDATA="$ROOT/.pgdata"
PORT="${LAB_PGPORT:-55432}"
export LC_ALL=en_US.UTF-8
export LANG=en_US.UTF-8
case "${1:-}" in
  init)
    [ -d "$PGDATA" ] && { echo "이미 있음: $PGDATA"; exit 0; }
    initdb -D "$PGDATA" -U lab --auth=trust --encoding=UTF8 --locale=C >/dev/null
    ;;
  start)
    pg_ctl -D "$PGDATA" -l "$ROOT/.pgdata/server.log" -w \
      -o "-p $PORT -k /tmp -c shared_buffers=1GB -c effective_cache_size=4GB -c work_mem=64MB \
-c maintenance_work_mem=512MB -c jit=off -c random_page_cost=1.1 -c max_wal_size=1GB \
-c checkpoint_timeout=5min -c synchronous_commit=off" start
    ;;
  stop)   pg_ctl -D "$PGDATA" -m fast stop ;;
  status) pg_ctl -D "$PGDATA" status ;;
  psql)   shift; exec psql -h /tmp -p "$PORT" -U lab -d "${LAB_DB:-lab}" "$@" ;;
  *) echo "사용법: $0 {init|start|stop|status|psql}"; exit 1 ;;
esac
