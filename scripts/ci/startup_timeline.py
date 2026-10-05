"""Summary of the cold-start timeline Vola's debug build logs as "VolaStartup" (stage S3a)."""

from __future__ import annotations

import re
from statistics import median

MARK = re.compile(r"VolaStartup\s*:\s*mark (\S+) (\d+)")
TOOK = re.compile(r"VolaStartup\s*:\s*took (\S+) (\d+) (main|background)")
SLOW = re.compile(r"VolaStartup\s*:\s*slow (\d+) at (\d+) (.*)")
HASH = re.compile(r"[{@][0-9a-f]+\}?")

ORDER = (
    "ProviderCreated",
    "ActivityCreateStart",
    "BlockingListsLoadStart",
    "ActivityCreateEnd",
    "ActivityResumed",
    "FirstFrame",
    "BlockingListsReady",
    "FirstPageComposite",
)


def summarize(runs: list[str], top_slow: int = 8) -> list[str]:
    """Median of each mark and duration over the runs, then the slowest main-thread messages."""
    marks: dict[str, list[int]] = {}
    took: dict[tuple[str, str], list[int]] = {}
    slow: dict[str, int] = {}
    for run in runs:
        for line in run.splitlines():
            if match := MARK.search(line):
                marks.setdefault(match.group(1), []).append(int(match.group(2)))
            elif match := TOOK.search(line):
                took.setdefault((match.group(1), match.group(3)), []).append(int(match.group(2)))
            elif match := SLOW.search(line):
                # Hash codes differ per run; without them the same message adds up across runs.
                what = HASH.sub("", match.group(3)).strip()
                slow[what] = max(slow.get(what, 0), int(match.group(1)))
    if not marks and not took:
        return ["Startup timeline: no VolaStartup lines (not a debug build?)"]
    lines = [f"Startup timeline (median of {len(runs)} runs, ms since process start):"]
    names = [name for name in ORDER if name in marks] + sorted(set(marks) - set(ORDER))
    lines += [f"  {name}: {int(median(marks[name]))}" for name in names]
    if took:
        lines.append("Durations (median, ms):")
        lines += [
            f"  {name} ({thread}): {int(median(values))}"
            for (name, thread), values in sorted(took.items())
        ]
    if slow:
        lines.append(f"Slowest main-thread messages in the first 15 s (worst run, ms, top {top_slow}):")
        ranked = sorted(slow.items(), key=lambda item: -item[1])[:top_slow]
        lines += [f"  {millis}: {what}" for what, millis in ranked]
    return lines
