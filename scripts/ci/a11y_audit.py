#!/usr/bin/env python3
"""Checks Vola's own controls in the screenshot tour's UI dumps for touch size and labels.

capture_screenshots.py saves a uiautomator dump next to every screenshot. This script reads them
and reports every clickable Vola control that is smaller than 48 dp on either side, or that has no
text and no content description for TalkBack. Web content (GeckoView and WebView subtrees) and other
apps are skipped: the page is the site's, not Vola's.

uiautomator reports only the visible part of a control, so a control cut by the screen edge or by a
scrolling container (a peeking tab card, the last row of a sheet) is not size-checked: its size
depends on the scroll position, not on the design.

Usage:
  a11y_audit.py DUMPS_DIR PACKAGE --baseline FILE [--report FILE] [--write-baseline]

Known findings live in the baseline file, one key per line; only new ones fail the check. A dump
directory without a baseline file collects findings and passes, so the first run can seed it.
"""

import argparse
import re
import sys
import xml.etree.ElementTree as ElementTree
from pathlib import Path

MIN_TOUCH_DP = 48
# Rounding of bounds to whole pixels can shave a fraction of a dp off a 48 dp target.
TOUCH_TOLERANCE_DP = 0.5
WEB_CONTENT_CLASSES = ("GeckoView", "WebView")


def parse_bounds(value):
    numbers = [int(number) for number in re.findall(r"-?\d+", value or "")]
    return numbers if len(numbers) == 4 else None


def has_label(node):
    """A node or anything inside it carries text TalkBack can read."""
    return any(
        (child.get("text") or "").strip() or (child.get("content-desc") or "").strip()
        for child in node.iter("node")
    )


def clipped_axes(bounds, container):
    """(horizontally, vertically): whether bounds meet an edge of the container that may clip them."""
    near = [abs(side - edge) <= 1 for side, edge in zip(bounds, container)]
    return near[0] or near[2], near[1] or near[3]


def describe(node):
    label = (node.get("content-desc") or node.get("text") or "").strip()
    resource = (node.get("resource-id") or "").rsplit("/", 1)[-1]
    kind = (node.get("class") or "").rsplit(".", 1)[-1]
    return label or resource or kind


def audit(xml_text, package, density):
    """Findings as (kind, description, width dp, height dp) for one UI dump."""
    try:
        root = ElementTree.fromstring(xml_text)
    except ElementTree.ParseError:
        return []
    scale = density / 160
    findings = []

    def visit(node, in_web_content, container):
        node_class = node.get("class") or ""
        in_web_content = in_web_content or any(name in node_class for name in WEB_CONTENT_CLASSES)
        bounds = parse_bounds(node.get("bounds"))
        ours = node.get("package") == package and node.get("visible-to-user", "true") == "true"
        if ours and not in_web_content and node.get("clickable") == "true" and bounds:
            width = round((bounds[2] - bounds[0]) / scale)
            height = round((bounds[3] - bounds[1]) / scale)
            if width > 0 and height > 0:
                limit = MIN_TOUCH_DP - TOUCH_TOLERANCE_DP
                clipped_x, clipped_y = clipped_axes(bounds, container)
                if (width < limit and not clipped_x) or (height < limit and not clipped_y):
                    findings.append(("small", describe(node), width, height))
                if not has_label(node):
                    findings.append(("unlabeled", describe(node), width, height))
        if bounds and node.get("scrollable") == "true":
            container = bounds
        for child in node.findall("node"):
            visit(child, in_web_content, container)

    for top in root.findall("node"):
        visit(top, False, parse_bounds(top.get("bounds")) or [0, 0, 0, 0])
    return findings


def screen_group(screen):
    """The screen without its pass: tab-overview-dark and tab-overview-ru are one screen."""
    return re.sub(r"-(light|dark|ru|a11y)$", "", screen)


def finding_key(kind, description, width, height, screen):
    """Stable across runs. A small control is keyed by its size, so the same control on two screens
    is one finding; an unlabeled one by its screen, since a clipped size would change with scroll."""
    if kind == "small":
        return f"small | {description} | {width}x{height} dp"
    return f"unlabeled | {description} | {screen_group(screen)}"


def collect(dumps_dir, package, density):
    """Maps each finding key to the screens that show it."""
    found = {}
    for dump in sorted(Path(dumps_dir).glob("*.xml")):
        screen = re.sub(r"^\d+-", "", dump.stem)
        for finding in audit(dump.read_text(encoding="utf-8", errors="replace"), package, density):
            found.setdefault(finding_key(*finding, screen), []).append(screen)
    return found


def read_baseline(path):
    if not path.exists():
        return None
    return {line.strip() for line in path.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.startswith("#")}


def report_text(found, baseline):
    lines = [f"Vola controls in the tour: {len(found)} accessibility finding(s)."]
    for key in sorted(found):
        status = "known" if baseline is not None and key in baseline else "NEW"
        screens = ", ".join(sorted(set(found[key])))
        lines.append(f"- [{status}] {key} — {screens}")
    for key in sorted((baseline or set()) - set(found)):
        lines.append(f"- [gone] {key} — not seen in this tour; delete it from the baseline if fixed")
    return "\n".join(lines) + "\n"


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("dumps")
    parser.add_argument("package")
    parser.add_argument("--baseline", required=True, type=Path)
    parser.add_argument("--report", type=Path)
    parser.add_argument("--write-baseline", action="store_true")
    args = parser.parse_args(argv)

    density_file = Path(args.dumps) / "density.txt"
    density = int(density_file.read_text().strip()) if density_file.exists() else 420
    found = collect(args.dumps, args.package, density)
    baseline = read_baseline(args.baseline)
    report = report_text(found, baseline)
    print(report, end="")
    if args.report:
        args.report.write_text(report, encoding="utf-8")
    if args.write_baseline:
        header = "# Known accessibility findings in Vola's UI; scripts/ci/a11y_audit.py. Fix, then delete.\n"
        args.baseline.write_text(header + "".join(f"{key}\n" for key in sorted(found)), encoding="utf-8")
        return 0
    if baseline is None:
        print(f"No baseline at {args.baseline}: findings are collected, not enforced.")
        return 0
    new = sorted(key for key in found if key not in baseline)
    for key in new:
        print(f"::error::New accessibility finding: {key} (screens: {', '.join(sorted(set(found[key])))})")
    return 1 if new else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
