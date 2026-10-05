#!/usr/bin/env python3
"""조회 시간을 EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) 으로 측정한다.

사용법:  python3 bench/run.py --variants A,B [--runs 5]
결과:    results/timings.csv (누적), results/plans/<variant>_<query>.txt, results/answers/<variant>_<query>.txt
"""
import argparse, csv, json, os, statistics, subprocess, sys

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
sys.path.insert(0, os.path.join(ROOT, "bench"))
from queries import QUERIES, VARIANTS  # noqa: E402

PORT = os.environ.get("LAB_PGPORT", "55432")
BASE = ["psql", "-h", "/tmp", "-p", PORT, "-U", "lab", "-d", "lab", "-X", "-At", "-v", "ON_ERROR_STOP=1"]
HEADER = ["variant", "variant_desc", "query", "query_desc", "median_ms", "min_ms", "max_ms",
          "runs", "planning_ms", "shared_hit", "shared_read", "partitions_scanned", "scan_nodes", "indexes_used"]


def psql(sql):
    r = subprocess.run(BASE + ["-c", "SET statement_timeout = '600s'; " + sql],
                       capture_output=True, text=True)
    if r.returncode != 0:
        raise RuntimeError(r.stderr.strip())
    return r.stdout


def walk(node, out):
    out.append(node)
    for ch in node.get("Plans", []):
        walk(ch, out)


def explain_json(sql):
    # SET 문 출력이 섞이지 않도록 JSON 부분만 잘라낸다.
    txt = psql("EXPLAIN (ANALYZE, BUFFERS, FORMAT JSON) " + sql)
    start = txt.index("[")
    return json.loads(txt[start:])[0]


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--variants", required=True)
    ap.add_argument("--runs", type=int, default=5)
    a = ap.parse_args()

    os.makedirs(os.path.join(ROOT, "results", "plans"), exist_ok=True)
    os.makedirs(os.path.join(ROOT, "results", "answers"), exist_ok=True)
    csv_path = os.path.join(ROOT, "results", "timings.csv")
    rows = []
    for v in a.variants.split(","):
        table, vdesc = VARIANTS[v]
        for name, qdesc, sql_raw, sql_roll in QUERIES:
            sql = (sql_raw.format(t=table) if table else sql_roll)
            if sql is None:
                continue
            sql = " ".join(sql.split())
            # 정답 파일(변형 간 결과 일치 검증용)
            ans = psql(sql).splitlines()
            ans = [l for l in ans if l != "SET"]      # SET 문 출력 제거
            open(os.path.join(ROOT, "results", "answers", f"{v}_{name}.txt"), "w").write("\n".join(ans) + "\n")
            # 워밍업 1회 + 측정 N회
            explain_json(sql)
            times, last = [], None
            for _ in range(a.runs):
                last = explain_json(sql)
                times.append(last["Execution Time"])
            nodes = []
            walk(last["Plan"], nodes)
            rels = {n["Relation Name"] for n in nodes if "Relation Name" in n}
            parts = {r for r in rels if r.startswith("obs_part_")}
            idx = sorted({n["Index Name"] for n in nodes if "Index Name" in n})
            scans = sorted({n["Node Type"] for n in nodes if "Scan" in n["Node Type"]})
            rows.append([v, vdesc, name, qdesc,
                         round(statistics.median(times), 1), round(min(times), 1), round(max(times), 1),
                         a.runs, round(last["Planning Time"], 2),
                         last["Plan"].get("Shared Hit Blocks", 0), last["Plan"].get("Shared Read Blocks", 0),
                         len(parts), "/".join(scans), "/".join(idx)])
            plan_txt = psql("EXPLAIN (ANALYZE, BUFFERS) " + sql)
            plan_txt = plan_txt[plan_txt.index("QUERY PLAN") + 10:] if "QUERY PLAN" in plan_txt else plan_txt
            open(os.path.join(ROOT, "results", "plans", f"{v}_{name}.txt"), "w").write(plan_txt)
            print(f"[{v}] {name} median={rows[-1][4]} ms  min={rows[-1][5]}  parts={len(parts)}  scans={rows[-1][12]}", flush=True)

    # 같은 variant 결과는 덮어쓰고 나머지는 보존
    old = []
    if os.path.exists(csv_path):
        with open(csv_path, newline="") as f:
            r = csv.reader(f)
            next(r, None)
            old = [x for x in r if x and x[0] not in a.variants.split(",")]
    with open(csv_path, "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(HEADER)
        w.writerows(sorted(old + [[str(c) for c in r] for r in rows], key=lambda x: (x[0], x[2])))


if __name__ == "__main__":
    main()
