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

    def test_rounds_are_combined_by_median(self):
        with tempfile.TemporaryDirectory() as directory:
            paths = [Path(directory) / f"{index}.json" for index in range(3)]
            # The third round is the spike seen on #400 (747.8 against 647.7): the median ignores it.
            for path, browser, tab in zip(paths, (650.0, 660.0, 750.0), (2.0, -1.0, -21.0)):
                path.write_text(json.dumps({"browser_mb": browser, "per_tab_mb": tab}))
            self.assertEqual(memory_budget.combined([*paths, Path(directory) / "absent.json"]),
                             {"browser_mb": 660.0, "per_tab_mb": -1.0})
            self.assertEqual(memory_budget.combined([Path(directory) / "absent.json"]), {})

    def test_within_budget_passes(self):
        _, errors, warnings = memory_budget.compare({"browser_mb": 660, "per_tab_mb": 1},
                                                    {"browser_mb": 695, "per_tab_mb": 2}, approved=False)
        self.assertEqual((errors, warnings), ([], []))

    def test_browser_regression_only_warns_unless_enforced(self):
        base, head = {"browser_mb": 660, "per_tab_mb": 1}, {"browser_mb": 760, "per_tab_mb": 1}
        _, errors, warnings = memory_budget.compare(base, head, approved=False)
        self.assertEqual((errors, warnings), ([], ["browser with one page, MB went from 660.0 to 760.0"]))

    def test_enforced_regression_fails_unless_approved(self):
        base, head = {"browser_mb": 660, "per_tab_mb": 1}, {"browser_mb": 760, "per_tab_mb": 1}
        _, errors, _ = memory_budget.compare(base, head, approved=False, enforce=True)
        self.assertEqual(errors, ["browser with one page, MB went from 660.0 to 760.0"])
        self.assertEqual(memory_budget.compare(base, head, approved=True, enforce=True)[1], [])

    def test_noise_seen_in_calibration_is_not_a_regression(self):
        # #403 touched tests only and moved the browser by +74.9 MB on two rounds a build; the
        # threshold needs both the ratio and the absolute growth, so 10 % of a 300 MB browser is not enough.
        _, errors, warnings = memory_budget.compare({"browser_mb": 300, "per_tab_mb": 1},
                                                    {"browser_mb": 339, "per_tab_mb": 1}, approved=False,
                                                    enforce=True)
        self.assertEqual((errors, warnings), ([], []))

    def test_what_a_tab_adds_is_reported_but_never_flagged(self):
        # #400: -9.9 against 1.3 MB per tab flagged the old budget, though the tab's number is noise.
        lines, errors, warnings = memory_budget.compare({"browser_mb": 660, "per_tab_mb": -9.9},
                                                        {"browser_mb": 660, "per_tab_mb": 40.0},
                                                        approved=False, enforce=True)
        self.assertEqual((errors, warnings), ([], []))
        self.assertIn("| each further tab, MB | ≤ 40 | -9.9 | 40.0 | +49.9 |", lines)

    def test_missing_pull_request_result_fails(self):
        _, errors, _ = memory_budget.compare({"browser_mb": 200, "per_tab_mb": 30}, {}, approved=True)
        self.assertEqual(len(errors), 2)

    def test_the_first_ci_run_does_not_fail(self):
        # Base 737 / 938 MB with negative growth per tab: noise the gate must not fail on yet.
        base = {"browser_mb": 838.0, "per_tab_mb": -12.7}
        head = {"browser_mb": 747.4, "per_tab_mb": -2.8}
        lines, errors, warnings = memory_budget.compare(base, head, approved=False)
        self.assertEqual((errors, warnings), ([], []))
        self.assertTrue(any(line.startswith("A regression only warns") for line in lines))

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
