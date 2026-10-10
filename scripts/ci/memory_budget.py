#!/usr/bin/env python3
"""Memory budget (docs/vola/plan-v5.md, section 7, V3): how much memory the browser takes with one
page open, and how much each further tab adds, for a pull request against its base.

Usage:
  memory_budget.py measure --package PKG --url URL --out FILE [--tabs N]
  memory_budget.py compare --base FILE [FILE ...] --head FILE [FILE ...] [--approved] [--enforce]

measure drives the emulator with adb (ANDROID_SERIAL): it opens URL as a link from another app (a
new tab each time), waits for the pages to settle and sums the PSS of every Vola process, Gecko's
content and GPU processes included. compare averages the rounds of each build and flags a pull
request that takes noticeably more memory. Until a week of numbers calibrates the budget it only
warns; --enforce turns a regression into a failure. On the first CI run the same build measured
737 and 938 MB, and memory one minute after start fell below the first sample, so the numbers are
not yet steady enough to fail a pull request on.
"""

from __future__ import annotations

import argparse
import json
import os
import re
import subprocess
import sys
import time
from pathlib import Path
from statistics import mean, median

APPROVAL_LABEL = "perf-approved"
TARGET_BROWSER_MB = 250.0
TARGET_PER_TAB_MB = 40.0
# A regression must exceed both the ratio and the absolute growth to count.
BROWSER_MAX_RATIO, BROWSER_MIN_DELTA_MB = 1.10, 15.0
PER_TAB_MAX_RATIO, PER_TAB_MIN_DELTA_MB = 1.15, 5.0
PAGE_SETTLE_SECONDS = 8
MEMORY_SETTLE_SECONDS = 25
SAMPLES = 3

PSS_LINE = re.compile(r"^\s*([\d,]+)K:\s+(\S+)\s+\(pid")


def process_pss_kb(meminfo: str, package: str) -> int:
    """Total PSS of [package]'s processes in the "Total PSS by process" part of `dumpsys meminfo`."""
    total = 0
    in_section = False
    for line in meminfo.splitlines():
        if line.startswith("Total PSS by process"):
            in_section = True
            continue
        if in_section:
            if not line.strip():
                break
            match = PSS_LINE.match(line)
            if match and (match.group(2) == package or match.group(2).startswith(package + ":")):
                total += int(match.group(1).replace(",", ""))
    return total


def adb(*args: str) -> str:
    return subprocess.run(["adb", *args], check=True, capture_output=True, text=True).stdout


def open_link(package: str, url: str) -> None:
    adb("shell", "am", "start", "-W", "-a", "android.intent.action.VIEW", "-d", url, "-p", package)
    time.sleep(PAGE_SETTLE_SECONDS)


def sample_pss_kb(package: str) -> float:
    time.sleep(MEMORY_SETTLE_SECONDS)
    samples = []
    for _ in range(SAMPLES):
        samples.append(process_pss_kb(adb("shell", "dumpsys", "meminfo"), package))
        time.sleep(2)
    return median(samples)


def measure(package: str, url: str, tabs: int) -> dict[str, float]:
    adb("shell", "am", "force-stop", package)
    open_link(package, f"{url}?tab=0")
    one_tab = sample_pss_kb(package)
    for tab in range(1, tabs + 1):
        open_link(package, f"{url}?tab={tab}")
    all_tabs = sample_pss_kb(package)
    return {"browser_mb": one_tab / 1024, "per_tab_mb": (all_tabs - one_tab) / tabs / 1024}


def averaged(paths: list[Path]) -> dict[str, float]:
    rounds = [json.loads(path.read_text()) for path in paths if path.is_file()]
    if not rounds:
        return {}
    return {key: mean(round_[key] for round_ in rounds) for key in ("browser_mb", "per_tab_mb")}


def compare(base: dict[str, float], head: dict[str, float], approved: bool,
            enforce: bool = False) -> tuple[list[str], list[str], list[str]]:
    """The summary lines, the errors that fail the job and the warnings that only annotate it."""
    lines = [
        "### Memory budget (emulator, base vs this pull request)",
        "",
        "| Budget | Target | Base | This PR | Change |",
        "| --- | ---: | ---: | ---: | ---: |",
    ]
    errors: list[str] = []
    warnings: list[str] = []
    budgets = (
        ("browser with one page, MB", "browser_mb", TARGET_BROWSER_MB, BROWSER_MAX_RATIO, BROWSER_MIN_DELTA_MB),
        ("each further tab, MB", "per_tab_mb", TARGET_PER_TAB_MB, PER_TAB_MAX_RATIO, PER_TAB_MIN_DELTA_MB),
    )
    for label, key, target, max_ratio, min_delta in budgets:
        before, after = base.get(key), head.get(key)
        if after is None:
            errors.append(f"no {label} result for this pull request")
            lines.append(f"| {label} | ≤ {target:.0f} | {fmt(before)} | — | — |")
            continue
        worse = before is not None and after > before * max_ratio and after - before > min_delta
        change = "—" if before is None else f"{after - before:+.1f}"
        mark = " ⚠️" if worse else ""
        lines.append(f"| {label} | ≤ {target:.0f} | {fmt(before)} | {after:.1f} | {change}{mark} |")
        if worse and not approved:
            (errors if enforce else warnings).append(f"{label} went from {before:.1f} to {after:.1f}")
    lines.append("")
    lines.append(
        f"A pull request fails when the browser grows by over {(BROWSER_MAX_RATIO - 1) * 100:.0f} % and "
        f"{BROWSER_MIN_DELTA_MB:.0f} MB, or each tab by over {(PER_TAB_MAX_RATIO - 1) * 100:.0f} % and "
        f"{PER_TAB_MIN_DELTA_MB:.0f} MB. Targets are reported until a week of numbers calibrates them."
    )
    if not enforce:
        lines.append("Calibration: a regression only warns for now; it fails the job once the budget "
                     "is enforced.")
    if errors and not approved:
        lines.append(f"To let a known regression through, the owner adds the `{APPROVAL_LABEL}` label "
                     "and re-runs this job.")
    return lines, errors, warnings


def fmt(value: float | None) -> str:
    return "—" if value is None else f"{value:.1f}"


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    measure_parser = commands.add_parser("measure")
    measure_parser.add_argument("--package", required=True)
    measure_parser.add_argument("--url", required=True)
    measure_parser.add_argument("--out", required=True, type=Path)
    measure_parser.add_argument("--tabs", type=int, default=5)
    compare_parser = commands.add_parser("compare")
    compare_parser.add_argument("--base", nargs="+", type=Path, required=True)
    compare_parser.add_argument("--head", nargs="+", type=Path, required=True)
    compare_parser.add_argument("--approved", action="store_true")
    compare_parser.add_argument("--enforce", action="store_true")
    args = parser.parse_args(argv)

    if args.command == "measure":
        result = measure(args.package, args.url, args.tabs)
        args.out.parent.mkdir(parents=True, exist_ok=True)
        args.out.write_text(json.dumps(result, indent=2) + "\n")
        print(json.dumps(result))
        return 0

    lines, errors, warnings = compare(averaged(args.base), averaged(args.head), args.approved, args.enforce)
    text = "\n".join(lines) + "\n"
    print(text)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(text)
    for warning in warnings:
        print(f"::warning title=Memory budget::{warning}")
    for error in errors:
        print(f"::error title=Memory budget::{error}")
    return 1 if errors else 0


if __name__ == "__main__":
    sys.exit(main())
