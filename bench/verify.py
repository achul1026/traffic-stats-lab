#!/usr/bin/env python3
"""변형(A~E)이 같은 질의에 같은 답을 내는지 검증한다. 기준은 A(단일 테이블·인덱스 없음)."""
import glob, os, sys
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ANS = os.path.join(ROOT, "results", "answers")

def load(path):
    return [line.split("|") for line in open(path).read().strip().splitlines() if line != "SET"]

def same(a, b, tol=0.011):
    if len(a) != len(b):
        return False
    for ra, rb in zip(a, b):
        if len(ra) != len(rb):
            return False
        for x, y in zip(ra, rb):
            try:
                if abs(float(x) - float(y)) > tol:
                    return False
            except ValueError:
                if x != y:
                    return False
    return True

ok = True
lines = ["| 질의 | 변형 | 기준(A) 대비 |", "|---|---|---|"]
for q in ("Q1", "Q2", "Q3", "Q4"):
    base = load(os.path.join(ANS, f"A_{q}.txt"))
    for v in "BCDE":
        p = os.path.join(ANS, f"{v}_{q}.txt")
        if not os.path.exists(p):
            continue
        r = same(base, load(p))
        ok &= r
        lines.append(f"| {q} | {v} | {'일치' if r else '**불일치**'} (행 {len(base)}) |")
open(os.path.join(ROOT, "results", "verification.md"), "w").write("\n".join(lines) + "\n")
print("\n".join(lines)); print("전체:", "OK" if ok else "FAIL")
sys.exit(0 if ok else 1)
