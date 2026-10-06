#!/usr/bin/env python3
"""Summarizes androidTest JUnit XML from the instrumented.yml shards for the run page.

Usage: instrumented_summary.py SHARD_DIR [SHARD_DIR ...] [--baseline FILE] [--markdown FILE]

Every SHARD_DIR is searched for JUnit XML. A shard without any XML failed before its tests ran,
which fails the run. A failing test that is not listed in the baseline fails the run too; listed
tests are reported as known failures, and listed tests that passed are reported as fixed so their
entries can be removed (docs/vola/ci-instrumented-tests.md).

Baseline lines are "package.Class#method" or "package.Class" (every method of the class);
text after " -- " is a note, lines starting with "#" are comments.
"""

import argparse
import sys
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path

MAX_LISTED_FAILURES = 60
STACK_LINES = 8


@dataclass(frozen=True)
class TestResult:
    class_name: str
    method: str
    seconds: float
    outcome: str  # "passed", "failed" or "skipped"
    message: str = ""
    details: str = ""

    @property
    def test_id(self) -> str:
        return f"{self.class_name}#{self.method}"


@dataclass
class Summary:
    results: list
    empty_shards: list
    shard_seconds: dict


def read_results(path: Path) -> list:
    root = ElementTree.parse(path).getroot()
    suites = [root] if root.tag == "testsuite" else root.iter("testsuite")
    results = []
    for suite in suites:
        for case in suite.iter("testcase"):
            failure = case.find("failure")
            if failure is None:
                failure = case.find("error")
            if failure is not None:
                text = (failure.text or "").strip()
                message = (failure.get("message") or "").strip()
                if not message:
                    message = text.splitlines()[0] if text else "no message"
                outcome, details = "failed", text
            elif case.find("skipped") is not None:
                outcome, message, details = "skipped", "", ""
            else:
                outcome, message, details = "passed", "", ""
            results.append(
                TestResult(
                    class_name=case.get("classname", "?"),
                    method=case.get("name", "?"),
                    seconds=float(case.get("time") or 0),
                    outcome=outcome,
                    message=message,
                    details=details,
                )
            )
    return results


def collect(shard_dirs: list) -> Summary:
    results, empty_shards, shard_seconds = [], [], {}
    for shard in shard_dirs:
        files = sorted(Path(shard).rglob("*.xml"))
        shard_results = []
        for file in files:
            try:
                shard_results.extend(read_results(file))
            except ElementTree.ParseError:
                continue
        if not shard_results:
            empty_shards.append(str(shard))
        shard_seconds[str(shard)] = sum(result.seconds for result in shard_results)
        results.extend(shard_results)
    return Summary(results=results, empty_shards=empty_shards, shard_seconds=shard_seconds)


def read_baseline(path) -> set:
    if path is None or not Path(path).is_file():
        return set()
    entries = set()
    for line in Path(path).read_text(encoding="utf-8").splitlines():
        entry = line.split(" -- ", 1)[0].strip()
        if entry and not entry.startswith("#"):
            entries.add(entry)
    return entries


def is_known(result: TestResult, baseline: set) -> bool:
    return result.test_id in baseline or result.class_name in baseline


def short_name(class_name: str) -> str:
    return class_name.rsplit(".", 1)[-1]


def format_minutes(seconds: float) -> str:
    return f"{int(seconds // 60)} min {int(seconds % 60)} s"


def render(summary: Summary, baseline: set) -> tuple:
    """Returns (markdown lines, exit status)."""
    results = summary.results
    failed = [result for result in results if result.outcome == "failed"]
    new_failures = [result for result in failed if not is_known(result, baseline)]
    known_failures = [result for result in failed if is_known(result, baseline)]
    fixed = sorted(
        result.test_id
        for result in results
        if result.outcome == "passed" and result.test_id in baseline
    )
    passed = sum(1 for result in results if result.outcome == "passed")
    skipped = sum(1 for result in results if result.outcome == "skipped")
    total_seconds = sum(result.seconds for result in results)

    lines = ["### Instrumented tests", ""]
    lines.append("| Tests | Passed | Failed (new) | Failed (known) | Skipped | Test time |")
    lines.append("|---|---|---|---|---|---|")
    lines.append(
        f"| {len(results)} | {passed} | {len(new_failures)} | {len(known_failures)} | "
        f"{skipped} | {format_minutes(total_seconds)} |"
    )
    lines.append("")
    if len(summary.shard_seconds) > 1:
        shard_times = ", ".join(
            f"{Path(shard).name} {format_minutes(seconds)}"
            for shard, seconds in sorted(summary.shard_seconds.items())
        )
        lines.extend([f"Shards: {shard_times}.", ""])
    for shard in summary.empty_shards:
        lines.extend([f"**{Path(shard).name}: no test results** (build, install or emulator failed).", ""])

    if new_failures:
        lines.extend([f"#### New failures ({len(new_failures)})", ""])
        lines.extend(render_failures(new_failures))
    if known_failures:
        by_class = {}
        for result in known_failures:
            by_class.setdefault(result.class_name, []).append(result.method)
        lines.extend([f"<details><summary>Known failures ({len(known_failures)})</summary>", ""])
        for class_name in sorted(by_class):
            lines.append(f"- `{short_name(class_name)}`: {len(by_class[class_name])}")
        lines.extend(["", "</details>", ""])
    if fixed:
        lines.extend([f"#### Fixed — remove from the baseline ({len(fixed)})", ""])
        lines.extend(f"- `{test_id}`" for test_id in fixed)
        lines.append("")

    status = 1 if new_failures or summary.empty_shards or not results else 0
    if not results:
        lines.append("**No test results at all.**")
    return lines, status


def render_failures(failures: list) -> list:
    lines = []
    for result in sorted(failures, key=lambda item: item.test_id)[:MAX_LISTED_FAILURES]:
        message = result.message[:300]
        lines.append(
            f"<details><summary><code>{short_name(result.class_name)}#{result.method}</code> — "
            f"{escape_html(message)}</summary>"
        )
        lines.append("")
        lines.append("```")
        lines.append(result.test_id)
        lines.extend(result.details.splitlines()[:STACK_LINES])
        lines.append("```")
        lines.append("</details>")
    if len(failures) > MAX_LISTED_FAILURES:
        lines.append(f"- … and {len(failures) - MAX_LISTED_FAILURES} more (see the report artifacts).")
    lines.append("")
    return lines


def escape_html(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("shards", nargs="+", help="directories with the JUnit XML of one shard each")
    parser.add_argument("--baseline", help="known failures, one test per line")
    parser.add_argument("--markdown", help="append the summary to this file (e.g. $GITHUB_STEP_SUMMARY)")
    parser.add_argument("--failures", help="write every failing test id to this file")
    args = parser.parse_args(argv)

    summary = collect(args.shards)
    baseline = read_baseline(args.baseline)
    lines, status = render(summary, baseline)
    text = "\n".join(lines) + "\n"
    print(text)
    if args.markdown:
        with open(args.markdown, "a", encoding="utf-8") as output:
            output.write(text)
    if args.failures:
        failing = sorted({result.test_id for result in summary.results if result.outcome == "failed"})
        Path(args.failures).write_text("".join(f"{test_id}\n" for test_id in failing), encoding="utf-8")
    return status


if __name__ == "__main__":
    sys.exit(main())
