#!/usr/bin/env python3
"""Memory budget (docs/vola/plan-v5.md, section 7, V3): how much memory the browser takes with one
page open, and how much each further tab adds, for a pull request against its base.

Usage:
  memory_budget.py measure --package PKG --url URL --out FILE [--tabs N]
  memory_budget.py compare --base FILE [FILE ...] --head FILE [FILE ...] [--approved] [--enforce]

measure drives the emulator with adb (ANDROID_SERIAL): it opens URL as a link from another app (a
new tab each time), waits for the pages to settle and sums the PSS of every Vola process, Gecko's
content and GPU processes included. compare takes the median of each build's rounds and flags a
pull request that takes noticeably more memory. It only warns; --enforce turns a regression into a
failure.

Calibration (13 runs, 2026-10-16 and 17): two rounds of the same build differed by 28 MB at the
median and by up to 100 MB, and pull requests that touch no app code moved the browser by up to
+75 MB, so the browser's threshold sits above that noise and the rounds went from two to three.
What a further tab adds stayed between -21 and +8 MB on the small fixture page: below the noise,
so it is reported but never flagged until a measurement that loads each tab replaces it.

measure also keeps what the browser's number is made of: each Vola process's PSS and its
`dumpsys meminfo <pid>` App Summary (Java heap, native heap, code, graphics...), so memory work can
aim at the biggest part instead of guessing. compare prints it for the median round of each build.
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
from statistics import median

APPROVAL_LABEL = "perf-approved"
TARGET_BROWSER_MB = 250.0
TARGET_PER_TAB_MB = 40.0
# A regression must exceed both the ratio and the absolute growth to count.
BROWSER_MAX_RATIO, BROWSER_MIN_DELTA_MB = 1.10, 40.0
PAGE_SETTLE_SECONDS = 8
MEMORY_SETTLE_SECONDS = 25
SAMPLES = 3

PSS_LINE = re.compile(r"^\s*([\d,]+)K:\s+(\S+)\s+\(pid (\d+)")
SUMMARY_LINE = re.compile(r"^\s*(Java Heap|Native Heap|Code|Stack|Graphics|Private Other|System):\s+([\d,]+)")
# "Unknown" is left out: the App Summary gives it an RSS only, which the PSS column would misread.
SUMMARY_PARTS = ("Java Heap", "Native Heap", "Code", "Graphics", "Stack", "Private Other", "System")


def package_processes(meminfo: str, package: str) -> list[tuple[str, int, int]]:
    """[package]'s processes as (name, pid, PSS kB), from the "Total PSS by process" part."""
    processes = []
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
                processes.append((match.group(2), int(match.group(3)), int(match.group(1).replace(",", ""))))
    return processes


def process_pss_kb(meminfo: str, package: str) -> int:
    """Total PSS of [package]'s processes in the "Total PSS by process" part of `dumpsys meminfo`."""
    return sum(pss for _, _, pss in package_processes(meminfo, package))


def app_summary_kb(process_meminfo: str) -> dict[str, int]:
    """The PSS column of the App Summary in `dumpsys meminfo <pid>`: Java heap, native heap, code..."""
    summary: dict[str, int] = {}
    in_summary = False
    for line in process_meminfo.splitlines():
        if line.strip() == "App Summary":
            in_summary = True
            continue
        if in_summary:
            match = SUMMARY_LINE.match(line)
            if match:
                summary[match.group(1)] = int(match.group(2).replace(",", ""))
            elif line.strip().startswith("TOTAL"):
                break
    return summary


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


def breakdown(package: str) -> list[dict]:
    """What the browser's number is made of right now: each process's PSS and its App Summary, in MB."""
    parts = []
    for name, pid, pss in package_processes(adb("shell", "dumpsys", "meminfo"), package):
        try:
            summary = app_summary_kb(adb("shell", "dumpsys", "meminfo", str(pid)))
        except subprocess.CalledProcessError:
            summary = {}  # the process went away between the two calls
        parts.append({
            "process": name.removeprefix(package) or "main",
            "pss_mb": round(pss / 1024, 1),
            "summary_mb": {key: round(value / 1024, 1) for key, value in summary.items()},
        })
    return parts


def measure(package: str, url: str, tabs: int) -> dict[str, float]:
    adb("shell", "am", "force-stop", package)
    open_link(package, f"{url}?tab=0")
    one_tab = sample_pss_kb(package)
    parts = breakdown(package)
    for tab in range(1, tabs + 1):
        open_link(package, f"{url}?tab={tab}")
    all_tabs = sample_pss_kb(package)
    return {"browser_mb": one_tab / 1024, "per_tab_mb": (all_tabs - one_tab) / tabs / 1024, "processes": parts}


def combined(paths: list[Path]) -> dict[str, float]:
    """The median of a build's rounds: one round the emulator slowed down does not move it."""
    rounds = [json.loads(path.read_text()) for path in paths if path.is_file()]
    if not rounds:
        return {}
    return {key: median(round_[key] for round_ in rounds) for key in ("browser_mb", "per_tab_mb")}


def median_round(paths: list[Path]) -> dict:
    """The round whose browser number is the median of the build's rounds: its breakdown is typical."""
    rounds = sorted((json.loads(path.read_text()) for path in paths if path.is_file()),
                    key=lambda round_: round_["browser_mb"])
    return rounds[(len(rounds) - 1) // 2] if rounds else {}


def breakdown_lines(base: dict, head: dict) -> list[str]:
    """What each build's one-page number is made of, process by process, for its median round."""
    lines: list[str] = []
    for title, round_ in (("This PR", head), ("Base", base)):
        processes = round_.get("processes")
        if not processes:
            continue
        lines += [
            "",
            f"#### What one page is made of — {title} (median round, MB)",
            "",
            "| Process | PSS | " + " | ".join(SUMMARY_PARTS) + " |",
            "| --- | ---: | " + " | ".join("---:" for _ in SUMMARY_PARTS) + " |",
        ]
        for part in sorted(processes, key=lambda item: -item["pss_mb"]):
            cells = " | ".join(fmt(part["summary_mb"].get(key)) for key in SUMMARY_PARTS)
            lines.append(f"| {part['process']} | {part['pss_mb']:.1f} | {cells} |")
    return lines


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
    # The tab's number is below the emulator's noise (see the module's docstring): reported only.
    budgets = (
        ("browser with one page, MB", "browser_mb", TARGET_BROWSER_MB, True),
        ("each further tab, MB", "per_tab_mb", TARGET_PER_TAB_MB, False),
    )
    for label, key, target, gated in budgets:
        before, after = base.get(key), head.get(key)
        if after is None:
            errors.append(f"no {label} result for this pull request")
            lines.append(f"| {label} | ≤ {target:.0f} | {fmt(before)} | — | — |")
            continue
        worse = (gated and before is not None and after > before * BROWSER_MAX_RATIO
                 and after - before > BROWSER_MIN_DELTA_MB)
        change = "—" if before is None else f"{after - before:+.1f}"
        mark = " ⚠️" if worse else ""
        lines.append(f"| {label} | ≤ {target:.0f} | {fmt(before)} | {after:.1f} | {change}{mark} |")
        if worse and not approved:
            (errors if enforce else warnings).append(f"{label} went from {before:.1f} to {after:.1f}")
    lines.append("")
    lines.append(
        f"Each build's number is the median of its rounds. The browser is flagged when it grows by over "
        f"{(BROWSER_MAX_RATIO - 1) * 100:.0f} % and {BROWSER_MIN_DELTA_MB:.0f} MB; what a further tab adds "
        f"is below the emulator's noise on the fixture page, so it is reported, not flagged."
    )
    if not enforce:
        lines.append("A regression only warns for now; it fails the job once the budget is enforced.")
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

    lines, errors, warnings = compare(combined(args.base), combined(args.head), args.approved, args.enforce)
    lines += breakdown_lines(median_round(args.base), median_round(args.head))
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
