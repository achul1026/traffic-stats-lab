"""통계 화면에서 자주 쓰는 조회 4종. {t}는 대상 테이블로 치환된다."""

# 각 쿼리는 (이름, 설명, 원본 테이블용 SQL, 집계 테이블용 SQL 또는 None)
QUERIES = [
    ("Q1", "특정 지역의 최근 3개월 월별 통행량",
     """SELECT to_char(obs_time AT TIME ZONE 'Asia/Seoul', 'YYYY-MM') AS m, sum(volume) AS vol
        FROM {t}
        WHERE region_id = 7
          AND obs_time >= '2024-10-01 00:00:00+09' AND obs_time < '2025-01-01 00:00:00+09'
        GROUP BY 1 ORDER BY 1""",
     """SELECT to_char(day, 'YYYY-MM') AS m, sum(total_volume) AS vol
        FROM daily_region_stats
        WHERE region_id = 7 AND day >= DATE '2024-10-01' AND day < DATE '2025-01-01'
        GROUP BY 1 ORDER BY 1"""),
    ("Q2", "전 지역 2024-11월 일별 평균 속도",
     """SELECT to_char(obs_time AT TIME ZONE 'Asia/Seoul', 'YYYY-MM-DD') AS d,
               round(avg(speed_kmh)::numeric, 2) AS avg_speed
        FROM {t}
        WHERE obs_time >= '2024-11-01 00:00:00+09' AND obs_time < '2024-12-01 00:00:00+09'
        GROUP BY 1 ORDER BY 1""",
     """SELECT to_char(day, 'YYYY-MM-DD') AS d,
               round((sum(sum_speed) / sum(samples))::numeric, 2) AS avg_speed
        FROM daily_region_stats
        WHERE day >= DATE '2024-11-01' AND day < DATE '2024-12-01'
        GROUP BY 1 ORDER BY 1"""),
    ("Q3", "특정 도로의 2024년 시간대별 평균 속도",
     """SELECT extract(hour FROM obs_time AT TIME ZONE 'Asia/Seoul')::int AS h,
               round(avg(speed_kmh)::numeric, 2) AS avg_speed
        FROM {t}
        WHERE road_id = 777
          AND obs_time >= '2024-01-01 00:00:00+09' AND obs_time < '2025-01-01 00:00:00+09'
        GROUP BY 1 ORDER BY 1""",
     None),   # 도로 단위는 집계 테이블에 없음
    ("Q4", "2024년 지역별 연간 통행량 순위",
     """SELECT region_id, sum(volume) AS vol
        FROM {t}
        WHERE obs_time >= '2024-01-01 00:00:00+09' AND obs_time < '2025-01-01 00:00:00+09'
        GROUP BY 1 ORDER BY 2 DESC, 1""",
     """SELECT region_id, sum(total_volume) AS vol
        FROM daily_region_stats
        WHERE day >= DATE '2024-01-01' AND day < DATE '2025-01-01'
        GROUP BY 1 ORDER BY 2 DESC, 1"""),
]

# 변형(variant) -> 대상 테이블 (None 이면 집계 테이블용 SQL 사용)
VARIANTS = {
    "A": ("obs_plain", "단일 테이블 · 인덱스 없음"),
    "B": ("obs_part",  "월 파티션 · 인덱스 없음"),
    "C": ("obs_plain", "단일 테이블 · 인덱스"),
    "D": ("obs_part",  "월 파티션 · 인덱스"),
    "E": (None,        "일 단위 집계 테이블"),
}
