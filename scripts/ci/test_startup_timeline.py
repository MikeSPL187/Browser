#!/usr/bin/env python3

import unittest

from startup_timeline import summarize


RUN_1 = """\
10-05 15:00:00.000  123  123 I VolaStartup: mark ProviderCreated 40
10-05 15:00:00.100  123  123 I VolaStartup: mark ActivityCreateStart 120
10-05 15:00:00.200  123  140 I VolaStartup: took GeckoRuntimeCreate 300 main
10-05 15:00:00.300  123  123 I VolaStartup: mark FirstFrame 900
10-05 15:00:00.400  123  123 I VolaStartup: slow 250 at 500 Handler (android.app.ActivityThread$H) {8a1b2c3} null: 159
"""
RUN_2 = """\
10-05 15:01:00.000  456  456 I VolaStartup: mark ProviderCreated 60
10-05 15:01:00.100  456  456 I VolaStartup: mark ActivityCreateStart 140
10-05 15:01:00.200  456  470 I VolaStartup: took GeckoRuntimeCreate 340 main
10-05 15:01:00.300  456  456 I VolaStartup: mark FirstFrame 1100
10-05 15:01:00.400  456  456 I VolaStartup: slow 310 at 520 Handler (android.app.ActivityThread$H) {4d5e6f} null: 159
"""


class StartupTimelineTest(unittest.TestCase):
    def test_medians_in_startup_order_and_slow_messages_merged_across_runs(self):
        lines = summarize([RUN_1, RUN_2])

        self.assertEqual("Startup timeline (median of 2 runs, ms since process start):", lines[0])
        self.assertEqual(["  ProviderCreated: 50", "  ActivityCreateStart: 130", "  FirstFrame: 1000"], lines[1:4])
        self.assertIn("  GeckoRuntimeCreate (main): 320", lines)
        slow = [line for line in lines if line.startswith("  310:")]
        self.assertEqual(1, len(slow))
        self.assertIn("ActivityThread$H", slow[0])

    def test_a_build_without_the_timeline_says_so(self):
        self.assertEqual(
            ["Startup timeline: no VolaStartup lines (not a debug build?)"],
            summarize(["nothing here"]),
        )


if __name__ == "__main__":
    unittest.main()
