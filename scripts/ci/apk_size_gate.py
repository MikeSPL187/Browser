#!/usr/bin/env python3
"""APK size budget (docs/vola/plan-v5.md, section 7): a pull request may not grow an APK by more
than LIMIT_BYTES against the last green build of main, unless the owner labels it size-approved.

Usage:
  apk_size_gate.py measure --out sizes.json NAME=APK [NAME=APK ...]
  apk_size_gate.py compare --current sizes.json [--reference main.json] [--approved]

Each push to main uploads its measured sizes; a pull request downloads them as the reference. With
no reference (first run, expired artifact) the gate only reports. The table goes to stdout and, in
Actions, to the job summary; a breach is an error annotation and exit status 1.
"""

from __future__ import annotations

import argparse
import json
import os
import sys
from pathlib import Path

LIMIT_BYTES = 200 * 1024
APPROVAL_LABEL = "size-approved"


def megabytes(size: int) -> str:
    return f"{size / (1024 * 1024):.2f} MB"


def signed_kilobytes(delta: int) -> str:
    return f"{delta / 1024:+.0f} KB"


def measure(pairs: list[str]) -> dict[str, int]:
    sizes = {}
    for pair in pairs:
        name, separator, path = pair.partition("=")
        if not separator or not name or not path:
            raise SystemExit(f"expected NAME=APK, got {pair!r}")
        sizes[name] = Path(path).stat().st_size
    return sizes


def compare(current: dict[str, int], reference: dict[str, int] | None, approved: bool,
            limit: int = LIMIT_BYTES) -> tuple[list[str], list[str]]:
    """The summary lines and the names of APKs that grew past the limit."""
    lines = [
        "### APK size budget",
        "",
        "| APK | Size | main | Change |",
        "| --- | ---: | ---: | ---: |",
    ]
    breaches = []
    for name, size in sorted(current.items()):
        base = (reference or {}).get(name)
        if base is None:
            lines.append(f"| {name} | {megabytes(size)} | — | — |")
            continue
        delta = size - base
        mark = ""
        if delta > limit:
            breaches.append(name)
            mark = " ⚠️"
        lines.append(f"| {name} | {megabytes(size)} | {megabytes(base)} | {signed_kilobytes(delta)}{mark} |")
    lines.append("")
    if reference is None:
        lines.append("No size from main yet, so nothing to compare: the gate only reports this time.")
    elif breaches and approved:
        lines.append(f"Growth over {limit // 1024} KB is approved by the `{APPROVAL_LABEL}` label.")
    elif breaches:
        lines.append(
            f"Growth over {limit // 1024} KB needs the owner's `{APPROVAL_LABEL}` label "
            "(add it, then re-run this job)."
        )
    else:
        lines.append(f"Every APK is within {limit // 1024} KB of main.")
    return lines, [] if approved else breaches


def main(argv: list[str] | None = None) -> int:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    commands = parser.add_subparsers(dest="command", required=True)
    measure_parser = commands.add_parser("measure")
    measure_parser.add_argument("--out", required=True, type=Path)
    measure_parser.add_argument("apks", nargs="+", metavar="NAME=APK")
    compare_parser = commands.add_parser("compare")
    compare_parser.add_argument("--current", required=True, type=Path)
    compare_parser.add_argument("--reference", type=Path)
    compare_parser.add_argument("--approved", action="store_true")
    args = parser.parse_args(argv)

    if args.command == "measure":
        args.out.write_text(json.dumps(measure(args.apks), indent=2, sort_keys=True) + "\n")
        return 0

    current = json.loads(args.current.read_text())
    reference = None
    if args.reference and args.reference.is_file():
        reference = json.loads(args.reference.read_text())
    lines, breaches = compare(current, reference, args.approved)
    report = "\n".join(lines) + "\n"
    print(report)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(report)
    for name in breaches:
        print(f"::error title=APK size budget::{name} grew by "
              f"{signed_kilobytes(current[name] - reference[name])} against main "
              f"(limit {LIMIT_BYTES // 1024} KB without the {APPROVAL_LABEL} label)")
    return 1 if breaches else 0


if __name__ == "__main__":
    sys.exit(main())
