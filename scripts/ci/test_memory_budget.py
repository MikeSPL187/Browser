#!/usr/bin/env python3
"""Tests for memory_budget.py: reading `dumpsys meminfo` and the memory budgets."""

import json
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import memory_budget  # noqa: E402

PACKAGE = "io.github.mikespl187.vola.benchmark"
MEMINFO = f"""\
Applications Memory Usage (in Kilobytes):
Uptime: 123 Realtime: 123

Total PSS by process:
    210,000K: {PACKAGE} (pid 4100 / activities)
    150,500K: {PACKAGE}:tab0 (pid 4200)
     40,250K: {PACKAGE}:gpu (pid 4300)
    120,000K: com.android.systemui (pid 900)
     30,000K: {PACKAGE}.other (pid 5000)

Total PSS by OOM adjustment:
    999,999K: {PACKAGE} (pid 4100 / activities)
"""


class MemoryBudgetTest(unittest.TestCase):
    def test_sums_every_process_of_the_package_only(self):
        self.assertEqual(memory_budget.process_pss_kb(MEMINFO, PACKAGE), 400_750)

    def test_no_section_means_nothing(self):
        self.assertEqual(memory_budget.process_pss_kb("nothing here", PACKAGE), 0)

    def test_rounds_are_averaged(self):
        with tempfile.TemporaryDirectory() as directory:
            first, second = Path(directory) / "1.json", Path(directory) / "2.json"
            first.write_text(json.dumps({"browser_mb": 200.0, "per_tab_mb": 30.0}))
            second.write_text(json.dumps({"browser_mb": 220.0, "per_tab_mb": 34.0}))
            self.assertEqual(memory_budget.averaged([first, second, Path(directory) / "absent.json"]),
                             {"browser_mb": 210.0, "per_tab_mb": 32.0})
            self.assertEqual(memory_budget.averaged([Path(directory) / "absent.json"]), {})

    def test_within_budget_passes(self):
        _, errors, warnings = memory_budget.compare({"browser_mb": 200, "per_tab_mb": 30},
                                          {"browser_mb": 214, "per_tab_mb": 34}, approved=False)
        self.assertEqual((errors, warnings), ([], []))

    def test_per_tab_regression_only_warns_while_calibrating(self):
        base, head = {"browser_mb": 200, "per_tab_mb": 30}, {"browser_mb": 200, "per_tab_mb": 40}
        _, errors, warnings = memory_budget.compare(base, head, approved=False)
        self.assertEqual((errors, warnings), ([], ["each further tab, MB went from 30.0 to 40.0"]))

    def test_enforced_regression_fails_unless_approved(self):
        base, head = {"browser_mb": 200, "per_tab_mb": 30}, {"browser_mb": 200, "per_tab_mb": 40}
        _, errors, _ = memory_budget.compare(base, head, approved=False, enforce=True)
        self.assertEqual(errors, ["each further tab, MB went from 30.0 to 40.0"])
        self.assertEqual(memory_budget.compare(base, head, approved=True, enforce=True)[1], [])

    def test_small_absolute_growth_is_noise(self):
        _, errors, warnings = memory_budget.compare({"browser_mb": 100, "per_tab_mb": 10},
                                                    {"browser_mb": 114, "per_tab_mb": 14}, approved=False,
                                                    enforce=True)
        self.assertEqual((errors, warnings), ([], []))

    def test_missing_pull_request_result_fails(self):
        _, errors, _ = memory_budget.compare({"browser_mb": 200, "per_tab_mb": 30}, {}, approved=True)
        self.assertEqual(len(errors), 2)

    def test_the_first_ci_run_does_not_fail(self):
        # Base 737 / 938 MB with negative growth per tab: noise the gate must not fail on yet.
        base = {"browser_mb": 838.0, "per_tab_mb": -12.7}
        head = {"browser_mb": 747.4, "per_tab_mb": -2.8}
        lines, errors, warnings = memory_budget.compare(base, head, approved=False)
        self.assertEqual(errors, [])
        self.assertEqual(len(warnings), 1)
        self.assertTrue(any(line.startswith("Calibration") for line in lines))

    def test_compare_command_prints_targets(self):
        with tempfile.TemporaryDirectory() as directory:
            base, head = Path(directory) / "base.json", Path(directory) / "head.json"
            base.write_text(json.dumps({"browser_mb": 200.0, "per_tab_mb": 30.0}))
            head.write_text(json.dumps({"browser_mb": 201.0, "per_tab_mb": 30.5}))
            output = StringIO()
            with redirect_stdout(output):
                status = memory_budget.main(["compare", "--base", str(base), "--head", str(head)])
            self.assertEqual(status, 0)
            self.assertIn("| browser with one page, MB | ≤ 250 | 200.0 | 201.0 | +1.0 |", output.getvalue())


if __name__ == "__main__":
    unittest.main()
