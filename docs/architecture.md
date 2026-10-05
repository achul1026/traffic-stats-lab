# 구성

```
          ┌──────────┐   GET /api/stats/*    ┌───────────────┐
 client ─▶│ Spring   │──────────────────────▶│ CachedStats   │── 캐시 적중 ──▶ Redis (TTL 5분)
 (ab)     │ Boot API │                       │ (@Cacheable)  │
          │ :8080    │                       └──────┬────────┘
          └────┬─────┘                              │ 캐시 미스 / cache=false / 브레이커 열림
               │ /actuator/prometheus               ▼
               ▼                          ┌──────────────────────┐
         Prometheus ─▶ Grafana            │ StatsRepository      │
                                          │  RAW    : obs_part   │ 월 파티션 + 인덱스 (PostgreSQL)
                                          │  ROLLUP : daily_region_stats │ 일 단위 집계
                                          └──────────────────────┘
```

- `source=RAW|ROLLUP`, `cache=true|false` 쿼리 파라미터로 같은 통계를 세 가지 방식(원본, 집계, 집계+캐시)으로 조회해 비교한다.
- `CacheBreaker`: 캐시 오류가 나면 10초간 캐시를 건너뛰고 DB 로 직접 조회한다.
- 지표: `http_server_requests`(처리량·지연 분포), `hikaricp_*`(커넥션 풀), `jvm_*`.

## 엔드포인트

| 경로 | 설명 | 대응하는 DB 질의 |
|---|---|---|
| `GET /api/stats/regions/{regionId}/monthly-volume?from=2024-10&to=2024-12` | 지역 월별 통행량 | Q1 |
| `GET /api/stats/daily-avg-speed?month=2024-11` | 전 지역 일별 평균 속도 | Q2 |
| `GET /api/stats/volume-ranking?year=2024` | 지역별 연간 통행량 순위 | Q4 |

공통 파라미터: `source`(기본 ROLLUP), `cache`(기본 true). 잘못된 입력은 400.
