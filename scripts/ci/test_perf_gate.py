#!/usr/bin/env python3
"""Tests for perf_gate.py: reading Macrobenchmark results and the cold start and frame budgets."""

import json
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import perf_gate  # noqa: E402

STARTUP = "dev.sk2andy.materialbrowser.baselineprofile.StartupBenchmark"


def startup_result(runs):
    return {"name": "coldStartWithProfileIfBundled", "className": STARTUP,
            "metrics": {"timeToInitialDisplayMs": {"median": runs[len(runs) // 2], "runs": runs}}}


def scroll_result(runs):
    return {"name": "scrollLongPage", "metrics": {},
            "sampledMetrics": {"frameOverrunMs": {"P50": 0, "runs": runs},
                               "frameDurationCpuMs": {"P50": 5, "runs": runs}}}


def write_round(directory, *benchmarks):
    directory.mkdir(parents=True, exist_ok=True)
    (directory / "dev.sk2andy.materialbrowser.baselineprofile-benchmarkData.json").write_text(
        json.dumps({"context": {}, "benchmarks": list(benchmarks)}))


class PerfGateTest(unittest.TestCase):
    def test_rounds_are_pooled(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            write_round(root / "1", startup_result([500, 510]))
            write_round(root / "2", startup_result([520]))
            self.assertEqual(perf_gate.load(root)["coldStartWithProfileIfBundled"]["timeToInitialDisplayMs"],
                             [500, 510, 520])

    def test_jank_percent_counts_overrunning_frames(self):
        self.assertEqual(perf_gate.jank_percent([[-5, 3], [-1, -2]]), 25.0)
        self.assertIsNone(perf_gate.jank_percent([]))

    def test_startup_within_budget_passes(self):
        rows = perf_gate.evaluate({"s": {"timeToInitialDisplayMs": [500, 500, 500]}},
                                  {"s": {"timeToInitialDisplayMs": [540, 545, 548]}})
        self.assertFalse(rows[0]["worse"])

    def test_startup_regression_fails(self):
        rows = perf_gate.evaluate({"s": {"timeToInitialDisplayMs": [500]}},
                                  {"s": {"timeToInitialDisplayMs": [600]}})
        self.assertTrue(rows[0]["worse"])
        _, errors = perf_gate.report(rows, approved=False)
        self.assertEqual(errors, ["s: cold start, median ms went from 500.0 to 600.0"])

    def test_small_absolute_change_is_noise(self):
        rows = perf_gate.evaluate({"s": {"timeToInitialDisplayMs": [100]}},
                                  {"s": {"timeToInitialDisplayMs": [125]}})
        self.assertFalse(rows[0]["worse"])

    def test_jank_regression_fails_unless_approved(self):
        base = {"scroll": {"frameOverrunMs": [[-1] * 90 + [1] * 10]}}
        head = {"scroll": {"frameOverrunMs": [[-1] * 80 + [1] * 20]}}
        rows = perf_gate.evaluate(base, head)
        self.assertEqual((rows[0]["base"], rows[0]["head"], rows[0]["worse"]), (10.0, 20.0, True))
        self.assertEqual(len(perf_gate.report(rows, approved=False)[1]), 1)
        lines, errors = perf_gate.report(rows, approved=True)
        self.assertEqual(errors, [])
        self.assertTrue(any("approved" in line for line in lines))

    def test_jank_change_under_two_points_is_noise(self):
        rows = perf_gate.evaluate({"scroll": {"frameOverrunMs": [[-1] * 99 + [1]]}},
                                  {"scroll": {"frameOverrunMs": [[-1] * 98 + [1] * 2]}})
        self.assertFalse(rows[0]["worse"])

    def test_a_missing_result_for_the_pull_request_fails(self):
        rows = perf_gate.evaluate({"s": {"timeToInitialDisplayMs": [500]}}, {})
        _, errors = perf_gate.report(rows, approved=True)
        self.assertEqual(errors, ["s: no cold start, median ms result for this pull request"])

    def test_no_results_at_all_fails(self):
        self.assertEqual(perf_gate.report([], approved=False)[1], ["no benchmark results were found"])

    def test_main_reads_both_builds(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            write_round(root / "base" / "1", startup_result([500, 500]), scroll_result([[-1, 1]]))
            write_round(root / "head" / "1", startup_result([505, 505]), scroll_result([[-1, 1]]))
            output = StringIO()
            with redirect_stdout(output):
                status = perf_gate.main(["--base", str(root / "base"), "--head", str(root / "head")])
            self.assertEqual(status, 0)
            self.assertIn("| coldStartWithProfileIfBundled | cold start, median ms | 500.0 | 505.0 | +5.0 |",
                          output.getvalue())
            self.assertIn("| scrollLongPage | janky frames, % | 50.0 | 50.0 | +0.0 |", output.getvalue())


if __name__ == "__main__":
    unittest.main()
