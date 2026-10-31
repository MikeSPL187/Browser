#!/usr/bin/env python3
"""
Gecko's own memory report (what about:memory shows) from the browser on the CI emulator.

`dumpsys meminfo` sees the main process as ~220 MB of "Unknown": Gecko's heap, which Android cannot
look into. Gecko can: its memory-info dumper (`nsMemoryInfoDumper`) answers a real-time signal by
collecting the memory reporters of every Gecko process into one `memory-report-*.json.gz`, the file
about:memory's «Measure and save» writes. This script makes it do that and summarises the file.

capture: becomes root on the emulator (`adb root`; Google APIs images allow it), reads the browser's
  main process, and signals it only if the process catches the dumper's three neighbouring real-time
  signals (dump, dump after minimizing, GC/CC log) starting at bionic's SIGRTMIN, which the device
  itself names (`kill -l RTMIN`). Otherwise it says the dumper is not there and sends nothing: a
  real-time signal nobody catches would end the process. Then it waits for the new report under the
  app's data directory and pulls it.

summarize: per process, the explicit total, heap-unclassified, resident and the largest explicit
  subtrees two and three levels deep, as Markdown for the job summary.

The report carries no page content: paths name allocation sites, and `anonymize` is on.
"""

from __future__ import annotations

import argparse
import gzip
import json
import re
import subprocess
import sys
import time
from collections import defaultdict
from pathlib import Path

REPORT_TIMEOUT_SECONDS = 90
POLL_SECONDS = 3
BYTES = 0  # nsIMemoryReporter.UNITS_BYTES
MB = 1024 * 1024


def adb(*args: str, check: bool = True) -> str:
    return subprocess.run(["adb", *args], check=check, capture_output=True, text=True).stdout


def shell(command: str, check: bool = True) -> str:
    return adb("shell", command, check=check).strip()


def become_root() -> bool:
    adb("root", check=False)
    adb("wait-for-device", check=False)
    return shell("id -u", check=False) == "0"


def caught_signals(pid: str) -> set[int]:
    """The signals [pid] handles itself, from SigCgt in /proc/<pid>/status (bit n-1 is signal n)."""
    status = shell(f"cat /proc/{pid}/status", check=False)
    match = re.search(r"^SigCgt:\s*([0-9a-fA-F]+)", status, re.MULTILINE)
    if not match:
        return set()
    mask = int(match.group(1), 16)
    return {bit + 1 for bit in range(64) if mask >> bit & 1}


def dumper_signal(pid: str) -> int | None:
    """SIGRTMIN when [pid] catches it and the two after it, as Gecko's memory-info dumper does; else None."""
    named = shell("kill -l RTMIN", check=False)
    if not named.isdigit():
        return None
    rtmin = int(named)
    caught = caught_signals(pid)
    return rtmin if {rtmin, rtmin + 1, rtmin + 2} <= caught else None


def reports_on_device(package: str) -> set[str]:
    found = shell(f"find /data/data/{package} /data/local/tmp -name 'memory-report-*.json.gz' 2>/dev/null", check=False)
    return {line.strip() for line in found.splitlines() if line.strip()}


def capture(package: str, out: Path) -> int:
    if not become_root():
        print("Gecko memory report: no root on this emulator; nothing captured.")
        return 0
    pid = shell(f"pidof {package}", check=False).split(" ")[0]
    if not pid.isdigit():
        print(f"Gecko memory report: {package} is not running.")
        return 0
    signal = dumper_signal(pid)
    if signal is None:
        print(f"Gecko memory report: process {pid} does not catch the memory-info dumper's signals; nothing sent.")
        return 0
    before = reports_on_device(package)
    shell(f"kill -{signal} {pid}")
    deadline = time.monotonic() + REPORT_TIMEOUT_SECONDS
    while time.monotonic() < deadline:
        time.sleep(POLL_SECONDS)
        new = sorted(reports_on_device(package) - before)
        # The dumper writes under a temporary name and renames it when complete.
        done = [path for path in new if not path.endswith(".tmp")]
        if done:
            out.parent.mkdir(parents=True, exist_ok=True)
            adb("pull", done[-1], str(out))
            print(f"Gecko memory report: signal {signal}, {done[-1]} -> {out}")
            return 0
    print(f"Gecko memory report: signal {signal} sent, no report within {REPORT_TIMEOUT_SECONDS} s.")
    return 0


def load_reports(path: Path) -> list[dict]:
    with gzip.open(path, "rt", encoding="utf-8") as file:
        return json.load(file)["reports"]


def process_name(process: str) -> str:
    """«Web Content (pid 1234)» → «Web Content»; the pid changes every run."""
    return re.sub(r"\s*\(pid \d+\)", "", process or "Main Process").strip() or "Main Process"


def summarize(path: Path, top: int) -> list[str]:
    explicit: dict[str, float] = defaultdict(float)
    other: dict[tuple[str, str], float] = defaultdict(float)
    subtrees: dict[tuple[str, str], float] = defaultdict(float)
    for report in load_reports(path):
        if report.get("units") != BYTES:
            continue
        process = process_name(report.get("process", ""))
        path_ = report.get("path", "")
        amount = report.get("amount", 0) / MB
        if path_.startswith("explicit/"):
            explicit[process] += amount
            parts = path_.split("/")
            # As about:memory's tree: «explicit/js-non-window» and one level under it.
            subtrees[(process, "/".join(parts[:2]))] += amount
            if len(parts) > 2:
                subtrees[(process, "/".join(parts[:3]))] += amount
        elif path_ in ("resident", "resident-unique", "heap-allocated", "heap-mapped", "js-main-runtime-gc-heap"):
            other[(process, path_)] += amount
    lines = [
        "### Gecko memory report (about:memory, MB)",
        "",
        "| Process | explicit | heap-unclassified | resident | resident-unique | heap-allocated |",
        "| --- | ---: | ---: | ---: | ---: | ---: |",
    ]
    unclassified = {process: subtrees.get((process, "explicit/heap-unclassified"), 0.0) for process in explicit}
    for process in sorted(explicit, key=explicit.get, reverse=True):
        lines.append(
            f"| {process} | {explicit[process]:.1f} | {unclassified[process]:.1f} | "
            f"{other.get((process, 'resident'), 0):.1f} | {other.get((process, 'resident-unique'), 0):.1f} | "
            f"{other.get((process, 'heap-allocated'), 0):.1f} |"
        )
    lines += ["", f"#### Largest explicit subtrees (top {top} per process)", "", "| Process | Path | MB |", "| --- | --- | ---: |"]
    for process in sorted(explicit, key=explicit.get, reverse=True):
        rows = sorted(
            ((tree, amount) for (owner, tree), amount in subtrees.items() if owner == process and amount >= 0.05),
            key=lambda row: row[1],
            reverse=True,
        )[:top]
        lines += [f"| {process} | `{tree}` | {amount:.1f} |" for tree, amount in rows]
    return lines


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    capture_parser = commands.add_parser("capture")
    capture_parser.add_argument("--package", required=True)
    capture_parser.add_argument("--out", required=True, type=Path)
    summarize_parser = commands.add_parser("summarize")
    summarize_parser.add_argument("report", type=Path)
    summarize_parser.add_argument("--top", type=int, default=15)
    args = parser.parse_args(argv)
    if args.command == "capture":
        return capture(args.package, args.out)
    if not args.report.is_file():
        print("No Gecko memory report was captured.")
        return 0
    print("\n".join(summarize(args.report, args.top)))
    return 0


if __name__ == "__main__":
    sys.exit(main())
