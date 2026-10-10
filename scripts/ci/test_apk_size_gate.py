#!/usr/bin/env python3
"""Tests for apk_size_gate.py: measuring, the comparison against main and the approval label."""

import json
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from io import StringIO
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import apk_size_gate  # noqa: E402

MB = 1024 * 1024
LIMIT = apk_size_gate.LIMIT_BYTES


class ApkSizeGateTest(unittest.TestCase):
    def test_growth_within_the_limit_passes(self):
        lines, breaches = apk_size_gate.compare({"gecko": 100 * MB + LIMIT}, {"gecko": 100 * MB}, approved=False)
        self.assertEqual(breaches, [])
        self.assertIn("| gecko | 100.20 MB | 100.00 MB | +200 KB |", lines)

    def test_growth_past_the_limit_fails(self):
        _, breaches = apk_size_gate.compare({"gecko": 100 * MB + LIMIT + 1, "webview": 15 * MB},
                                            {"gecko": 100 * MB, "webview": 15 * MB}, approved=False)
        self.assertEqual(breaches, ["gecko"])

    def test_the_label_approves_growth(self):
        lines, breaches = apk_size_gate.compare({"gecko": 101 * MB}, {"gecko": 100 * MB}, approved=True)
        self.assertEqual(breaches, [])
        self.assertTrue(any("approved" in line for line in lines))

    def test_shrinking_passes(self):
        lines, breaches = apk_size_gate.compare({"gecko": 99 * MB}, {"gecko": 100 * MB}, approved=False)
        self.assertEqual(breaches, [])
        self.assertIn("| gecko | 99.00 MB | 100.00 MB | -1024 KB |", lines)

    def test_without_a_reference_the_gate_only_reports(self):
        lines, breaches = apk_size_gate.compare({"gecko": 200 * MB}, None, approved=False)
        self.assertEqual(breaches, [])
        self.assertIn("| gecko | 200.00 MB | — | — |", lines)

    def test_an_apk_new_to_main_is_not_compared(self):
        _, breaches = apk_size_gate.compare({"gecko": 100 * MB, "new": 50 * MB}, {"gecko": 100 * MB}, approved=False)
        self.assertEqual(breaches, [])

    def test_measure_then_compare_exits_with_the_breach(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            apk = root / "app.apk"
            apk.write_bytes(b"x" * (LIMIT + 10))
            current = root / "current.json"
            reference = root / "main.json"
            apk_size_gate.main(["measure", "--out", str(current), f"gecko={apk}"])
            self.assertEqual(json.loads(current.read_text()), {"gecko": LIMIT + 10})
            reference.write_text(json.dumps({"gecko": 5}))
            output = StringIO()
            with redirect_stdout(output):
                status = apk_size_gate.main(["compare", "--current", str(current), "--reference", str(reference)])
            self.assertEqual(status, 1)
            self.assertIn("::error title=APK size budget::gecko grew by", output.getvalue())
            with redirect_stdout(StringIO()):
                missing = apk_size_gate.main(["compare", "--current", str(current),
                                              "--reference", str(root / "absent.json")])
            self.assertEqual(missing, 0)

    def test_a_malformed_pair_is_refused(self):
        with self.assertRaises(SystemExit):
            apk_size_gate.measure(["gecko"])


if __name__ == "__main__":
    unittest.main()
