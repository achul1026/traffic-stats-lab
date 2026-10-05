#!/usr/bin/env python3
"""results/timings.csv 로 results/chart.svg 와 results/results.md 를 만든다. (표준 라이브러리만 사용)"""
import csv, math, os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
R = os.path.join(ROOT, "results")
rows = list(csv.DictReader(open(os.path.join(R, "timings.csv"))))
V = ["A", "B", "C", "D", "E"]
VDESC = {r["variant"]: r["variant_desc"] for r in rows}
Q = sorted({r["query"] for r in rows})
QDESC = {r["query"]: r["query_desc"] for r in rows}
med = {(r["variant"], r["query"]): float(r["median_ms"]) for r in rows}
COL = {"A": "#9aa3ae", "B": "#5b6572", "C": "#e8a07f", "D": "#e8501f", "E": "#2e7d62"}

# ---- 표 ----
md = ["| 질의 | " + " | ".join(f"{v}<br>{VDESC[v]}" for v in V) + " |", "|---|" + "---|" * len(V)]
for q in Q:
    cells = []
    for v in V:
        if (v, q) not in med:
            cells.append("-"); continue
        t = med[(v, q)]
        s = "" if v == "A" else f" ({med[('A', q)] / t:.1f}배)"
        cells.append(f"{t:,.1f} ms{s}")
    md.append(f"| **{q}** {QDESC[q]} | " + " | ".join(cells) + " |")
open(os.path.join(R, "results.md"), "w").write("\n".join(md) + "\n")

# ---- 막대 그래프 (로그 축) ----
W, H, L, T, B = 980, 520, 70, 60, 110
lo, hi = 1, 10000
def y(ms): return T + (H - T - B) * (1 - (math.log10(max(ms, lo)) - 0) / (math.log10(hi) - 0))
gw = (W - L - 30) / len(Q); bw = gw / (len(V) + 1.2)
s = [f'<svg xmlns="http://www.w3.org/2000/svg" width="{W}" height="{H}" viewBox="0 0 {W} {H}" font-family="Noto Sans CJK KR, Malgun Gothic, sans-serif">',
     f'<rect width="{W}" height="{H}" fill="#fff"/>',
     f'<text x="{L}" y="28" font-size="18" font-weight="700" fill="#0e1116">질의별 조회 시간 (중앙값, 로그 축)</text>',
     f'<text x="{L}" y="48" font-size="12" fill="#5b6572">합성 데이터 3,000만 건 · 5회 측정 중앙값 · 낮을수록 빠름</text>']
for p in (1, 10, 100, 1000, 10000):
    yy = y(p)
    s += [f'<line x1="{L}" x2="{W-20}" y1="{yy:.1f}" y2="{yy:.1f}" stroke="#d9dce1" stroke-dasharray="3 3"/>',
          f'<text x="{L-8}" y="{yy+4:.1f}" font-size="11" text-anchor="end" fill="#5b6572">{p:,} ms</text>']
for i, q in enumerate(Q):
    gx = L + i * gw + gw * 0.08
    for j, v in enumerate(V):
        if (v, q) not in med: continue
        t = med[(v, q)]; x = gx + j * bw; yy = y(t)
        s += [f'<rect x="{x:.1f}" y="{yy:.1f}" width="{bw*0.9:.1f}" height="{H-B-yy:.1f}" fill="{COL[v]}"/>',
              f'<text x="{x+bw*0.45:.1f}" y="{yy-4:.1f}" font-size="10" text-anchor="middle" fill="#0e1116">{t:,.0f}</text>']
    s.append(f'<text x="{gx+bw*2.5:.1f}" y="{H-B+20}" font-size="14" font-weight="700" text-anchor="middle" fill="#0e1116">{q}</text>')
    s.append(f'<text x="{gx+bw*2.5:.1f}" y="{H-B+38}" font-size="11" text-anchor="middle" fill="#5b6572">{QDESC[q][:26]}</text>')
lx = L
for v in V:
    s += [f'<rect x="{lx}" y="{H-34}" width="12" height="12" fill="{COL[v]}"/>',
          f'<text x="{lx+18}" y="{H-24}" font-size="12" fill="#0e1116">{v} {VDESC[v]}</text>']
    lx += 180
s.append("</svg>")
open(os.path.join(R, "chart.svg"), "w").write("\n".join(s))
print(open(os.path.join(R, "results.md")).read())
