#!/usr/bin/env python3
"""ApacheBench(ab)로 API 부하 테스트를 하고 results/load/ 에 결과를 저장한다. (k6 대신 기본 설치된 ab 사용)

시나리오: 같은 통계를 (1) 원본 조회 (2) 집계 테이블 (3) 집계 테이블 + Redis 캐시 로 조회했을 때의 처리량·지연.
사용법: python3 bench/load.py [--n 300] [--c 20]
"""
import argparse, csv, os, re, subprocess, urllib.request

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
OUT = os.path.join(ROOT, "results", "load")
BASE = "http://localhost:8080/api/stats"

ENDPOINTS = {
    "Q1 월별 통행량": "/regions/7/monthly-volume?from=2024-10&to=2024-12",
    "Q2 일별 평균 속도": "/daily-avg-speed?month=2024-11",
    "Q4 지역 순위": "/volume-ranking?year=2024",
}
MODES = [
    ("원본 조회", "source=RAW&cache=false"),
    ("집계 테이블", "source=ROLLUP&cache=false"),
    ("집계 + Redis 캐시", "source=ROLLUP&cache=true"),
]


def run_ab(url, n, c):
    r = subprocess.run(["ab", "-n", str(n), "-c", str(c), "-q", url], capture_output=True, text=True)
    t = r.stdout
    g = lambda pat: float(re.search(pat, t).group(1))
    pct = {int(p): float(v) for p, v in re.findall(r"^\s*(\d+)%\s+(\d+)", t, re.M)}
    return {
        "rps": g(r"Requests per second:\s+([\d.]+)"),
        "mean_ms": g(r"Time per request:\s+([\d.]+) \[ms\] \(mean\)"),
        "p50": pct.get(50), "p95": pct.get(95), "p99": pct.get(99), "max": pct.get(100),
        "failed": int(re.search(r"Failed requests:\s+(\d+)", t).group(1)),
        "non2xx": int((re.search(r"Non-2xx responses:\s+(\d+)", t) or [0, 0])[1]),
        "raw": t,
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--n", type=int, default=300)
    ap.add_argument("--c", type=int, default=20)
    ap.add_argument("--label", default="")
    a = ap.parse_args()
    os.makedirs(OUT, exist_ok=True)
    rows = []
    for ename, path in ENDPOINTS.items():
        for mname, qs in MODES:
            url = f"{BASE}{path}&{qs}"
            urllib.request.urlopen(url).read()          # 워밍업(캐시 시나리오는 캐시 적재)
            m = run_ab(url, a.n, a.c)
            open(os.path.join(OUT, f"{a.label}{ename[:2]}_{mname.replace(' ', '').replace('+','')}.txt"), "w").write(m.pop("raw"))
            rows.append([ename, mname, a.n, a.c, m["rps"], m["mean_ms"], m["p50"], m["p95"], m["p99"], m["max"], m["failed"], m["non2xx"]])
            print(ename, mname, f"{m['rps']:.0f} req/s  p50={m['p50']}ms p95={m['p95']}ms p99={m['p99']}ms  failed={m['failed']}", flush=True)
    with open(os.path.join(OUT, f"{a.label}summary.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["endpoint", "mode", "requests", "concurrency", "req_per_s", "mean_ms", "p50_ms", "p95_ms", "p99_ms", "max_ms", "failed", "non2xx"])
        w.writerows(rows)


if __name__ == "__main__":
    main()
