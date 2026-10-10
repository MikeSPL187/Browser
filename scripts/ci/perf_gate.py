#!/usr/bin/env python3
"""Cold start and frame budgets (docs/vola/plan-v5.md, section 7): compares Macrobenchmark results
of a pull request ("head") with its base, both measured on the same emulator in one run.

Usage:
  perf_gate.py --base DIR --head DIR [--approved]

Each DIR holds the *benchmarkData.json files of every round for that build. A metric fails only
when it is worse relatively, absolutely and with statistical significance (one-sided Mann-Whitney
U test over all samples), because the same build's cold start varies by tens of percent from round
to round on a CI emulator. The owner's perf-approved label lets a known regression through.
"""

from __future__ import annotations

import argparse
import json
import math
import os
import sys
from pathlib import Path
from statistics import median

APPROVAL_LABEL = "perf-approved"
STARTUP_METRIC = "timeToInitialDisplayMs"
FRAME_METRIC = "frameDurationCpuMs"
# Cold start (median) may grow by at most 10 %, and a regression must also exceed 30 ms.
STARTUP_MAX_RATIO = 1.10
STARTUP_MIN_DELTA_MS = 30.0
# A frame's CPU time (90th percentile) may grow by at most 20 %, and by over 2 ms. On an emulator
# without a GPU every frame misses its deadline, so the share of late frames says nothing there.
FRAME_MAX_RATIO = 1.20
FRAME_MIN_DELTA_MS = 2.0
FRAME_PERCENTILE = 90
# How unlikely the slowdown must be as chance before it counts.
MAX_P_VALUE = 0.01


def load(directory: Path) -> dict[str, dict[str, list]]:
    """{benchmark name: {metric: runs}} over every result file under [directory]. A single-value
    metric's runs are numbers; a sampled metric's runs are lists (one per iteration)."""
    results: dict[str, dict[str, list]] = {}
    for path in sorted(directory.rglob("*benchmarkData.json")):
        for benchmark in json.loads(path.read_text()).get("benchmarks", []):
            metrics = results.setdefault(benchmark["name"], {})
            for group in ("metrics", "sampledMetrics"):
                for metric, values in benchmark.get(group, {}).items():
                    metrics.setdefault(metric, []).extend(values.get("runs", []))
    return results


def percentile(values: list[float], percent: float) -> float:
    """Linear-interpolated percentile of [values]."""
    ordered = sorted(values)
    position = (len(ordered) - 1) * percent / 100
    lower = math.floor(position)
    upper = min(lower + 1, len(ordered) - 1)
    return ordered[lower] + (ordered[upper] - ordered[lower]) * (position - lower)


def slower_p_value(base: list[float], head: list[float]) -> float:
    """One-sided Mann-Whitney U test: the chance of head looking this much slower than base when
    both come from the same build (normal approximation with a tie correction)."""
    n1, n2 = len(base), len(head)
    if not n1 or not n2:
        return 1.0
    pooled = sorted((value, group) for group, values in ((0, base), (1, head)) for value in values)
    ranks = [0.0] * len(pooled)
    ties = 0.0
    i = 0
    while i < len(pooled):
        j = i
        while j + 1 < len(pooled) and pooled[j + 1][0] == pooled[i][0]:
            j += 1
        for k in range(i, j + 1):
            ranks[k] = (i + j) / 2 + 1
        size = j - i + 1
        ties += size ** 3 - size
        i = j + 1
    head_rank_sum = sum(rank for rank, (_, group) in zip(ranks, pooled) if group == 1)
    u = head_rank_sum - n2 * (n2 + 1) / 2
    total = n1 + n2
    variance = n1 * n2 / 12 * ((total + 1) - ties / (total * (total - 1))) if total > 1 else 0.0
    if variance <= 0:
        return 1.0
    z = (u - n1 * n2 / 2 - 0.5) / math.sqrt(variance)
    return 0.5 * math.erfc(z / math.sqrt(2))


def budget_row(name: str, budget: str, base: list[float], head: list[float], statistic,
               max_ratio: float, min_delta: float) -> dict:
    before = statistic(base) if base else None
    after = statistic(head) if head else None
    p_value = slower_p_value(base, head)
    worse = (
        before is not None and after is not None
        and after > before * max_ratio and after - before > min_delta and p_value < MAX_P_VALUE
    )
    return {"benchmark": name, "budget": budget, "base": before, "head": after, "p": p_value,
            "worse": worse, "missing": after is None}


def evaluate(base: dict, head: dict) -> list[dict]:
    """One row per benchmark and budget: what was measured and whether it breaks the budget."""
    rows = []
    for name in sorted(set(base) | set(head)):
        base_metrics, head_metrics = base.get(name, {}), head.get(name, {})
        if STARTUP_METRIC in base_metrics or STARTUP_METRIC in head_metrics:
            rows.append(budget_row(
                name, "cold start, median ms",
                base_metrics.get(STARTUP_METRIC, []), head_metrics.get(STARTUP_METRIC, []),
                median, STARTUP_MAX_RATIO, STARTUP_MIN_DELTA_MS,
            ))
        if FRAME_METRIC in base_metrics or FRAME_METRIC in head_metrics:
            rows.append(budget_row(
                name, f"frame CPU time, P{FRAME_PERCENTILE} ms",
                [frame for run in base_metrics.get(FRAME_METRIC, []) for frame in run],
                [frame for run in head_metrics.get(FRAME_METRIC, []) for frame in run],
                lambda values: percentile(values, FRAME_PERCENTILE), FRAME_MAX_RATIO, FRAME_MIN_DELTA_MS,
            ))
    return rows


def number(value: float | None) -> str:
    return "—" if value is None else f"{value:.1f}"


def report(rows: list[dict], approved: bool) -> tuple[list[str], list[str]]:
    """The summary lines and the error annotations that fail the job."""
    lines = [
        "### Performance budget (emulator, base vs this pull request)",
        "",
        "| Benchmark | Budget | Base | This PR | Change | p |",
        "| --- | --- | ---: | ---: | ---: | ---: |",
    ]
    errors = []
    for row in rows:
        change = "—"
        if row["base"] is not None and row["head"] is not None:
            change = f"{row['head'] - row['base']:+.1f}"
        mark = " ⚠️" if row["worse"] else ""
        lines.append(f"| {row['benchmark']} | {row['budget']} | {number(row['base'])} | "
                     f"{number(row['head'])} | {change}{mark} | {row['p']:.3f} |")
        if row["missing"]:
            errors.append(f"{row['benchmark']}: no {row['budget']} result for this pull request")
        elif row["worse"] and not approved:
            errors.append(f"{row['benchmark']}: {row['budget']} went from {number(row['base'])} "
                          f"to {number(row['head'])}")
    if not rows:
        errors.append("no benchmark results were found")
    lines.append("")
    lines.append(
        f"Budgets: cold start at most +{(STARTUP_MAX_RATIO - 1) * 100:.0f} % (and over "
        f"{STARTUP_MIN_DELTA_MS:.0f} ms), frame CPU time at most +{(FRAME_MAX_RATIO - 1) * 100:.0f} % "
        f"(and over {FRAME_MIN_DELTA_MS:.0f} ms); a slowdown counts only when p < {MAX_P_VALUE} (the chance "
        "of seeing it between two identical builds). Emulator numbers compare builds; how smooth Vola "
        "feels is judged on a real phone."
    )
    if approved and any(row["worse"] for row in rows):
        lines.append(f"A regression is approved by the `{APPROVAL_LABEL}` label.")
    elif errors:
        lines.append(f"To let a known regression through, the owner adds the `{APPROVAL_LABEL}` label "
                     "and re-runs this job.")
    return lines, errors


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--base", required=True, type=Path)
    parser.add_argument("--head", required=True, type=Path)
    parser.add_argument("--approved", action="store_true")
    args = parser.parse_args(argv)

    lines, errors = report(evaluate(load(args.base), load(args.head)), args.approved)
    text = "\n".join(lines) + "\n"
    print(text)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(text)
    for error in errors:
        print(f"::error title=Performance budget::{error}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
