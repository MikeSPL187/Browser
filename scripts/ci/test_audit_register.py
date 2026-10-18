#!/usr/bin/env python3
"""The audit register lists every finding of the audit report, once."""

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
REPORT = ROOT / "docs/vola/audit-2026-10-18-full.md"
REGISTER = ROOT / "docs/vola/audit-2026-10-18-register.md"

ITEM = re.compile(r"^(?:- )?\*\*([A-Z]{1,3}-\d{2}) · ")
HEADING = re.compile(r"^### (N-\d{2}) · ([^·]+?) · (.+)$")
ROW = re.compile(r"^\| ([A-Z]{1,3}-\d{2}) \| ([^|]+) \| ([^|]+) \| ([^|]+) \| ([^|]+) \| ([^|]+) \| ([^|]+) \| ([^|]+) \|$")
CONTAINERS = ("Разбор", "Сценарии экранов")


def report_ids():
    ids = []
    for line in REPORT.read_text(encoding="utf-8").splitlines():
        heading = HEADING.match(line)
        if heading and not heading.group(2).startswith(CONTAINERS) and not any(c in heading.group(3) for c in CONTAINERS):
            ids.append(heading.group(1))
            continue
        item = ITEM.match(line)
        if item:
            ids.append(item.group(1))
    return ids


def register_rows():
    return [m.groups() for line in REGISTER.read_text(encoding="utf-8").splitlines() if (m := ROW.match(line))]


class AuditRegisterTest(unittest.TestCase):
    def test_every_finding_of_the_report_is_in_the_register(self):
        in_register = {row[0] for row in register_rows()}
        missing = [finding for finding in report_ids() if finding not in in_register]
        self.assertEqual([], missing, f"findings of the report without a register row: {missing}")

    def test_the_register_has_no_row_the_report_lacks(self):
        in_report = set(report_ids())
        extra = [row[0] for row in register_rows() if row[0] not in in_report]
        self.assertEqual([], extra, f"register rows without a finding in the report: {extra}")

    def test_no_finding_is_listed_twice(self):
        for name, ids in (("report", report_ids()), ("register", [row[0] for row in register_rows()])):
            duplicates = sorted({i for i in ids if ids.count(i) > 1})
            self.assertEqual([], duplicates, f"{name} lists twice: {duplicates}")

    def test_every_row_has_a_status(self):
        for row in register_rows():
            status = row[7].strip()
            self.assertTrue(
                status == "открыто" or status.startswith(("в работе", "исправлено", "отклонено")),
                f"{row[0]}: unknown status {status!r}",
            )

    def test_the_summary_counts_the_rows(self):
        text = REGISTER.read_text(encoding="utf-8")
        total = re.search(r"\| \*\*Всего\*\* \| \*\*(\d+)\*\* \|", text)
        self.assertIsNotNone(total)
        self.assertEqual(len(register_rows()), int(total.group(1)))


if __name__ == "__main__":
    unittest.main()
