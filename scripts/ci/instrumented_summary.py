#!/usr/bin/env python3
"""Summarizes androidTest JUnit XML from the instrumented.yml shards for the run page.

Usage: instrumented_summary.py SHARD_DIR [SHARD_DIR ...] [--baseline FILE] [--markdown FILE]
       [--failures FILE] [--new-failures FILE]

Every SHARD_DIR is searched for JUnit XML. A shard without any XML failed before its tests ran,
which fails the run. A failing test that is not listed in the baseline fails the run too; listed
tests are reported as known failures, and listed tests that passed are reported as fixed so their
entries can be removed (docs/vola/ci-instrumented-tests.md).

A shard reruns its new failures once (--new-failures lists them) and keeps those results in its
"retry" directory. A new failure that passed on the rerun is reported as flaky and does not fail
the run; one that failed again stays a new failure.

Baseline lines are "package.Class#method" or "package.Class" (every method of the class);
text after " -- " is a note, lines starting with "#" are comments.
"""

import argparse
import dataclasses
import re
import sys
import xml.etree.ElementTree as ElementTree
from dataclasses import dataclass
from pathlib import Path

MAX_LISTED_FAILURES = 200
STACK_LINES = 12
# A failure message can be a whole list (the screen audit files one finding per line): it is
# shown in full up to this many lines, before the stack frames.
MAX_MESSAGE_LINES = 300
LOG_LINES = 20
# Errors and crashes in a logcat line: "10-06 05:20:00.000  1234  1240 E Tag: message".
LOG_ERROR = re.compile(r"^\S+ \S+\s+\d+\s+\d+ [EF] |FATAL EXCEPTION")
ASSUMPTION_FAILURES = (
    "org.junit.AssumptionViolatedException",
    "org.junit.internal.AssumptionViolatedException",
)


@dataclass(frozen=True)
class TestResult:
    class_name: str
    method: str
    seconds: float
    outcome: str  # "passed", "failed", "skipped" or "flaky" (failed, then passed on the rerun)
    message: str = ""
    details: str = ""
    log: str = ""

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
            if failure is not None and is_assumption_failure(failure):
                outcome, message, details = "skipped", "", ""
            elif failure is not None:
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


def is_assumption_failure(failure) -> bool:
    """A test that skipped itself (assumeTrue) is reported by the test platform as a failure."""
    text = ((failure.get("message") or "") + " " + (failure.text or "")).lstrip()
    return text.startswith(ASSUMPTION_FAILURES) or (failure.get("type") or "").endswith(
        "AssumptionViolatedException"
    )


RETRY_DIR = "retry"


def first_run_files(shard: Path, pattern: str) -> list:
    return sorted(
        file
        for file in shard.rglob(pattern)
        if RETRY_DIR not in file.relative_to(shard).parts[:-1]
    )


def read_all(files: list) -> list:
    results = []
    for file in files:
        try:
            results.extend(read_results(file))
        except ElementTree.ParseError:
            continue
    return results


def collect(shard_dirs: list) -> Summary:
    results, empty_shards, shard_seconds = [], [], {}
    for shard in map(Path, shard_dirs):
        shard_results = read_all(first_run_files(shard, "*.xml"))
        if not shard_results:
            empty_shards.append(str(shard))
        passed_on_retry = {
            result.test_id
            for result in read_all(sorted((shard / RETRY_DIR).rglob("*.xml")))
            if result.outcome == "passed"
        }
        shard_results = [
            dataclasses.replace(result, outcome="flaky")
            if result.outcome == "failed" and result.test_id in passed_on_retry
            else result
            for result in shard_results
        ]
        shard_results = [attach_log(result, shard) for result in shard_results]
        shard_seconds[str(shard)] = sum(result.seconds for result in shard_results)
        results.extend(shard_results)
    return Summary(results=results, empty_shards=empty_shards, shard_seconds=shard_seconds)


def attach_log(result: TestResult, shard: Path) -> TestResult:
    """Adds the errors from the failed test's own logcat (logcat-<class>-<method>.txt)."""
    if result.outcome != "failed":
        return result
    name = f"{result.class_name}-{result.method}"
    for file in first_run_files(shard, "logcat-*.txt"):
        if file.name.endswith(f"{name}.txt"):
            lines = file.read_text(encoding="utf-8", errors="replace").splitlines()
            # The runner's own report repeats the stack shown above.
            errors = [
                line[:240]
                for line in lines
                if LOG_ERROR.search(line) and " TestRunner: " not in line
            ]
            return dataclasses.replace(result, log="\n".join(errors[-LOG_LINES:]))
    return result


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
    flaky = [result for result in results if result.outcome == "flaky"]
    passed = sum(1 for result in results if result.outcome == "passed")
    skipped = sum(1 for result in results if result.outcome == "skipped")
    total_seconds = sum(result.seconds for result in results)

    lines = ["### Instrumented tests", ""]
    lines.append(
        "| Tests | Passed | Flaky (passed on rerun) | Failed (new) | Failed (known) | Skipped "
        "| Test time |"
    )
    lines.append("|---|---|---|---|---|---|---|")
    lines.append(
        f"| {len(results)} | {passed} | {len(flaky)} | {len(new_failures)} | "
        f"{len(known_failures)} | {skipped} | {format_minutes(total_seconds)} |"
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
    if flaky:
        lines.extend([f"<details><summary>Flaky — failed, then passed on the rerun ({len(flaky)})</summary>", ""])
        for result in sorted(flaky, key=lambda item: item.test_id):
            message = escape_html(result.message[:160])
            lines.append(f"- `{short_name(result.class_name)}#{result.method}` — {message}")
        lines.extend(["", "</details>", ""])
    if known_failures:
        lines.extend([f"<details><summary>Known failures ({len(known_failures)})</summary>", ""])
        for result in sorted(known_failures, key=lambda item: item.test_id):
            message = escape_html(result.message[:160])
            lines.append(f"- `{short_name(result.class_name)}#{result.method}` — {message}")
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
        lines.extend(failure_lines(result.details))
        lines.append("```")
        if result.log:
            lines.extend(["", "Logcat errors:", "```", result.log, "```"])
        lines.append("</details>")
    if len(failures) > MAX_LISTED_FAILURES:
        lines.append(f"- … and {len(failures) - MAX_LISTED_FAILURES} more (see the report artifacts).")
    lines.append("")
    return lines


def failure_lines(details: str) -> list:
    """The failure's whole message, then the first STACK_LINES frames of its stack."""
    lines = details.splitlines()
    first_frame = next(
        (index for index, line in enumerate(lines) if line.lstrip().startswith("at ")),
        len(lines),
    )
    return lines[:first_frame][:MAX_MESSAGE_LINES] + lines[first_frame:first_frame + STACK_LINES]


def escape_html(text: str) -> str:
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def main(argv=None) -> int:
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("shards", nargs="+", help="directories with the JUnit XML of one shard each")
    parser.add_argument("--baseline", help="known failures, one test per line")
    parser.add_argument("--markdown", help="append the summary to this file (e.g. $GITHUB_STEP_SUMMARY)")
    parser.add_argument("--failures", help="write every failing test id to this file")
    parser.add_argument(
        "--new-failures",
        help="write the failing test ids that the baseline does not list to this file",
    )
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
    if args.new_failures:
        failing = sorted(
            {
                result.test_id
                for result in summary.results
                if result.outcome == "failed" and not is_known(result, baseline)
            }
        )
        Path(args.new_failures).write_text(
            "".join(f"{test_id}\n" for test_id in failing), encoding="utf-8"
        )
    return status


if __name__ == "__main__":
    sys.exit(main())
