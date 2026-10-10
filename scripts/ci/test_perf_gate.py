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

    def test_percentile_interpolates(self):
        self.assertEqual(perf_gate.percentile([1, 2, 3, 4, 5], 50), 3)
        self.assertAlmostEqual(perf_gate.percentile([10, 20], 90), 19.0)

    def test_p_value_separates_shifted_samples_from_noise(self):
        base = [500 + (i % 7) * 10 for i in range(30)]
        self.assertLess(perf_gate.slower_p_value(base, [value + 100 for value in base]), 0.001)
        self.assertGreater(perf_gate.slower_p_value(base, list(base)), 0.3)
        self.assertGreater(perf_gate.slower_p_value(base, [value - 100 for value in base]), 0.99)
        self.assertEqual(perf_gate.slower_p_value([], [1.0]), 1.0)

    def test_startup_within_budget_passes(self):
        rows = perf_gate.evaluate({"s": {"timeToInitialDisplayMs": [500, 500, 500]}},
                                  {"s": {"timeToInitialDisplayMs": [540, 545, 548]}})
        self.assertFalse(rows[0]["worse"])

    def test_significant_startup_regression_fails(self):
        base = [500 + i for i in range(20)]
        rows = perf_gate.evaluate({"s": {"timeToInitialDisplayMs": base}},
                                  {"s": {"timeToInitialDisplayMs": [value + 100 for value in base]}})
        self.assertTrue(rows[0]["worse"])
        _, errors = perf_gate.report(rows, approved=False)
        self.assertEqual(errors, ["s: cold start, median ms went from 509.5 to 609.5"])

    def test_a_large_but_noisy_difference_is_not_a_regression(self):
        # The same build measured 1654 and 2445 ms in two rounds on the CI emulator.
        base = [1650, 1700, 1900, 2100, 1680, 2300]
        head = [1600, 2350, 2400, 2450, 1700, 2200]
        rows = perf_gate.evaluate({"s": {"timeToInitialDisplayMs": base}}, {"s": {"timeToInitialDisplayMs": head}})
        self.assertGreater(rows[0]["head"], rows[0]["base"] * perf_gate.STARTUP_MAX_RATIO)
        self.assertFalse(rows[0]["worse"])

    def test_small_absolute_change_is_noise(self):
        base = [100 + i for i in range(20)]
        rows = perf_gate.evaluate({"s": {"timeToInitialDisplayMs": base}},
                                  {"s": {"timeToInitialDisplayMs": [value + 25 for value in base]}})
        self.assertFalse(rows[0]["worse"])

    def test_frame_regression_fails_unless_approved(self):
        base = {"scroll": {"frameDurationCpuMs": [[8.0 + (i % 5) for i in range(100)]]}}
        head = {"scroll": {"frameDurationCpuMs": [[14.0 + (i % 5) for i in range(100)]]}}
        rows = perf_gate.evaluate(base, head)
        self.assertEqual((rows[0]["base"], rows[0]["head"], rows[0]["worse"]), (12.0, 18.0, True))
        self.assertEqual(len(perf_gate.report(rows, approved=False)[1]), 1)
        lines, errors = perf_gate.report(rows, approved=True)
        self.assertEqual(errors, [])
        self.assertTrue(any("approved" in line for line in lines))

    def test_frame_change_under_two_ms_is_noise(self):
        rows = perf_gate.evaluate({"scroll": {"frameDurationCpuMs": [[5.0 + (i % 3) for i in range(100)]]}},
                                  {"scroll": {"frameDurationCpuMs": [[6.5 + (i % 3) for i in range(100)]]}})
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
            write_round(root / "base" / "1", startup_result([500, 500]), scroll_result([[4, 6]]))
            write_round(root / "head" / "1", startup_result([505, 505]), scroll_result([[4, 6]]))
            output = StringIO()
            with redirect_stdout(output):
                status = perf_gate.main(["--base", str(root / "base"), "--head", str(root / "head")])
            self.assertEqual(status, 0)
            self.assertIn("| coldStartWithProfileIfBundled | cold start, median ms | 500.0 | 505.0 | +5.0 |",
                          output.getvalue())
            self.assertIn("| scrollLongPage | frame CPU time, P90 ms | 5.8 | 5.8 | +0.0 |", output.getvalue())


if __name__ == "__main__":
    unittest.main()
