#!/usr/bin/env python3
"""Cold start and frame budgets (docs/vola/plan-v5.md, section 7): compares Macrobenchmark results
of a pull request ("head") with its base, both measured on the same emulator in one run.

Usage:
  perf_gate.py --base DIR --head DIR [--approved]

Each DIR holds the *benchmarkData.json files of every round for that build. A metric fails only
when it is worse both relatively and absolutely, so emulator noise on a tiny number does not turn
a pull request red. The owner's perf-approved label lets a known regression through.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path
from statistics import median

APPROVAL_LABEL = "perf-approved"
STARTUP_METRIC = "timeToInitialDisplayMs"
FRAME_METRIC = "frameOverrunMs"
# Cold start may grow by at most 10 %, and a regression must also exceed 30 ms to count.
STARTUP_MAX_RATIO = 1.10
STARTUP_MIN_DELTA_MS = 30.0
# The share of frames that miss their deadline may grow by at most 20 %, and by over 2 points.
JANK_MAX_RATIO = 1.20
JANK_MIN_DELTA_POINTS = 2.0


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


def jank_percent(runs: list[list[float]]) -> float | None:
    """Share of frames, over all iterations, that overran their deadline."""
    frames = [overrun for run in runs for overrun in run]
    if not frames:
        return None
    return 100.0 * sum(1 for overrun in frames if overrun > 0) / len(frames)


def evaluate(base: dict, head: dict) -> list[dict]:
    """One row per benchmark and budget: what was measured and whether it breaks the budget."""
    rows = []
    for name in sorted(set(base) | set(head)):
        base_metrics, head_metrics = base.get(name, {}), head.get(name, {})
        if STARTUP_METRIC in base_metrics or STARTUP_METRIC in head_metrics:
            before = median(base_metrics[STARTUP_METRIC]) if base_metrics.get(STARTUP_METRIC) else None
            after = median(head_metrics[STARTUP_METRIC]) if head_metrics.get(STARTUP_METRIC) else None
            worse = (
                before is not None and after is not None
                and after > before * STARTUP_MAX_RATIO and after - before > STARTUP_MIN_DELTA_MS
            )
            rows.append({"benchmark": name, "budget": "cold start, median ms", "base": before,
                         "head": after, "worse": worse, "missing": after is None})
        if FRAME_METRIC in base_metrics or FRAME_METRIC in head_metrics:
            before = jank_percent(base_metrics.get(FRAME_METRIC, []))
            after = jank_percent(head_metrics.get(FRAME_METRIC, []))
            worse = (
                before is not None and after is not None
                and after > before * JANK_MAX_RATIO and after - before > JANK_MIN_DELTA_POINTS
            )
            rows.append({"benchmark": name, "budget": "janky frames, %", "base": before,
                         "head": after, "worse": worse, "missing": after is None})
    return rows


def number(value: float | None) -> str:
    return "—" if value is None else f"{value:.1f}"


def report(rows: list[dict], approved: bool) -> tuple[list[str], list[str]]:
    """The summary lines and the error annotations that fail the job."""
    lines = [
        "### Performance budget (emulator, base vs this pull request)",
        "",
        "| Benchmark | Budget | Base | This PR | Change |",
        "| --- | --- | ---: | ---: | ---: |",
    ]
    errors = []
    for row in rows:
        change = "—"
        if row["base"] is not None and row["head"] is not None:
            change = f"{row['head'] - row['base']:+.1f}"
        mark = " ⚠️" if row["worse"] else ""
        lines.append(f"| {row['benchmark']} | {row['budget']} | {number(row['base'])} | "
                     f"{number(row['head'])} | {change}{mark} |")
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
        f"{STARTUP_MIN_DELTA_MS:.0f} ms), janky frames at most +{(JANK_MAX_RATIO - 1) * 100:.0f} % "
        f"(and over {JANK_MIN_DELTA_POINTS:.0f} points). Emulator numbers compare builds; how smooth "
        "Vola feels is judged on a real phone."
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
