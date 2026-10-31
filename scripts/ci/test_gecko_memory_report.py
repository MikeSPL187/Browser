import gzip
import json
import tempfile
import unittest
from pathlib import Path
from unittest import mock

import gecko_memory_report as report


class GeckoMemoryReportTest(unittest.TestCase):
    def write(self, reports):
        path = Path(tempfile.mkdtemp()) / "memory-report.json.gz"
        with gzip.open(path, "wt", encoding="utf-8") as file:
            json.dump({"version": 1, "reports": reports}, file)
        return path

    def test_summary_sums_processes_and_subtrees(self):
        mb = 1024 * 1024
        path = self.write([
            {"process": "Main Process (pid 10)", "path": "explicit/js-non-window/zones/x", "units": 0, "amount": 30 * mb},
            {"process": "Main Process (pid 10)", "path": "explicit/js-non-window/runtime", "units": 0, "amount": 10 * mb},
            {"process": "Main Process (pid 10)", "path": "explicit/heap-unclassified", "units": 0, "amount": 50 * mb},
            {"process": "Main Process (pid 10)", "path": "resident", "units": 0, "amount": 300 * mb},
            {"process": "Main Process (pid 10)", "path": "js-compartments/system", "units": 1, "amount": 99},
            {"process": "Web Content (pid 22)", "path": "explicit/images/content", "units": 0, "amount": 5 * mb},
        ])

        lines = report.summarize(path, top=5)
        text = "\n".join(lines)

        self.assertIn("| Main Process | 90.0 | 50.0 | 300.0 |", text)
        self.assertIn("| Web Content | 5.0 | 0.0 |", text)
        self.assertIn("| Main Process | `explicit/js-non-window` | 40.0 |", text)
        self.assertIn("| Main Process | `explicit/js-non-window/zones` | 30.0 |", text)
        self.assertNotIn("pid", text)

    def test_no_signal_is_sent_unless_the_dumper_catches_its_three(self):
        status = "Name:\tvola\nSigCgt:\t%x\n"
        caught_triple = (1 << 38) | (1 << 39) | (1 << 40)  # signals 39, 40, 41
        with mock.patch.object(report, "shell", side_effect=["39", status % caught_triple]):
            self.assertEqual(39, report.dumper_signal("123"))
        with mock.patch.object(report, "shell", side_effect=["39", status % (1 << 38)]):
            self.assertIsNone(report.dumper_signal("123"))
        with mock.patch.object(report, "shell", side_effect=["RTMIN?", ""]):
            self.assertIsNone(report.dumper_signal("123"))


if __name__ == "__main__":
    unittest.main()
