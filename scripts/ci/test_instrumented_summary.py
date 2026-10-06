#!/usr/bin/env python3

import tempfile
import unittest
from pathlib import Path

from instrumented_summary import collect, main, read_baseline, render

SHARD_0 = """\
<?xml version='1.0' encoding='UTF-8' ?>
<testsuite name="dev.sk2andy.materialbrowser" tests="3" failures="1" errors="0" skipped="1" time="12.5">
  <testcase name="opensMenu" classname="dev.sk2andy.materialbrowser.ui.MenuTest" time="2.0" />
  <testcase name="closesTab" classname="dev.sk2andy.materialbrowser.ui.TabsTest" time="3.5">
    <failure message="Expected &lt;1&gt; tabs">java.lang.AssertionError: Expected &lt;1&gt; tabs
	at dev.sk2andy.materialbrowser.ui.TabsTest.closesTab(TabsTest.kt:42)</failure>
  </testcase>
  <testcase name="onApi37" classname="dev.sk2andy.materialbrowser.ui.MenuTest" time="0">
    <skipped />
  </testcase>
  <testcase name="onTablet" classname="dev.sk2andy.materialbrowser.ui.MenuTest" time="0.5">
    <failure>org.junit.AssumptionViolatedException: got: &lt;false&gt;, expected: is &lt;true&gt;</failure>
  </testcase>
</testsuite>
"""

SHARD_1 = """\
<?xml version='1.0' encoding='UTF-8' ?>
<testsuites>
  <testsuite name="dev.sk2andy.materialbrowser" tests="2" time="7.0">
    <testcase name="loadsPage" classname="dev.sk2andy.materialbrowser.browser.gecko.GeckoTest" time="5.0">
      <error>java.lang.IllegalStateException: Test instrumentation process crashed.</error>
    </testcase>
    <testcase name="restores" classname="dev.sk2andy.materialbrowser.browser.SessionTest" time="2.0" />
  </testsuite>
</testsuites>
"""


class InstrumentedSummaryTest(unittest.TestCase):
    def setUp(self):
        self.directory = tempfile.TemporaryDirectory()
        root = Path(self.directory.name)
        self.shards = []
        for index, xml in enumerate([SHARD_0, SHARD_1]):
            shard = root / f"shard-{index}" / "connected" / "full"
            shard.mkdir(parents=True)
            (shard / "TEST-emulator.xml").write_text(xml, encoding="utf-8")
            self.shards.append(str(root / f"shard-{index}"))
        log_name = "logcat-dev.sk2andy.materialbrowser.ui.TabsTest-closesTab.txt"
        log = root / "shard-0" / "connected" / "full" / log_name
        log.write_text(
            "10-06 05:20:00.000  1234  1234 I TestRunner: started: closesTab\n"
            "10-06 05:20:01.000  1234  1240 E AndroidRuntime: FATAL EXCEPTION: main\n"
            "10-06 05:20:01.001  1234  1240 D Vola: noise\n"
            "10-06 05:20:02.000  1234  1240 E TestRunner: failed: closesTab\n",
            encoding="utf-8",
        )
        self.root = root

    def tearDown(self):
        self.directory.cleanup()

    def test_unlisted_failures_fail_the_run_with_their_message(self):
        lines, status = render(collect(self.shards), set())
        text = "\n".join(lines)

        self.assertEqual(1, status)
        self.assertIn("| 6 | 2 | 0 | 2 | 0 | 2 | 0 min 13 s |", text)
        self.assertIn("<code>TabsTest#closesTab</code> — Expected &lt;1&gt; tabs", text)
        self.assertIn("<code>GeckoTest#loadsPage</code> — java.lang.IllegalStateException", text)
        self.assertIn("TabsTest.kt:42", text)
        self.assertIn("E AndroidRuntime: FATAL EXCEPTION: main", text)
        self.assertNotIn("noise", text)
        self.assertNotIn("TestRunner: started", text)
        self.assertNotIn("TestRunner: failed", text)

    def test_baseline_entries_by_method_or_class_are_known_failures(self):
        baseline_file = self.root / "baseline.txt"
        baseline_file.write_text(
            "# known\n"
            "dev.sk2andy.materialbrowser.ui.TabsTest#closesTab -- menu changed in S4a\n"
            "dev.sk2andy.materialbrowser.browser.gecko.GeckoTest\n"
            "dev.sk2andy.materialbrowser.ui.MenuTest#opensMenu\n",
            encoding="utf-8",
        )
        lines, status = render(collect(self.shards), read_baseline(baseline_file))
        text = "\n".join(lines)

        self.assertEqual(0, status)
        self.assertIn("Known failures (2)", text)
        self.assertIn("`TabsTest#closesTab` — Expected &lt;1&gt; tabs", text)
        self.assertIn("- `dev.sk2andy.materialbrowser.ui.MenuTest#opensMenu`", text)
        self.assertNotIn("#### New failures", text)

    def test_a_shard_without_results_fails_the_run(self):
        empty = self.root / "shard-2"
        empty.mkdir()
        lines, status = render(collect(self.shards + [str(empty)]), {
            "dev.sk2andy.materialbrowser.ui.TabsTest",
            "dev.sk2andy.materialbrowser.browser.gecko.GeckoTest",
        })

        self.assertEqual(1, status)
        self.assertIn("**shard-2: no test results**", "\n".join(lines))

    def test_a_failure_that_passes_on_the_rerun_is_flaky(self):
        retry = self.root / "shard-0" / "retry" / "connected"
        retry.mkdir(parents=True)
        (retry / "TEST-emulator.xml").write_text(
            "<testsuite><testcase name=\"closesTab\" "
            "classname=\"dev.sk2andy.materialbrowser.ui.TabsTest\" time=\"3.0\" /></testsuite>",
            encoding="utf-8",
        )
        retry_1 = self.root / "shard-1" / "retry"
        retry_1.mkdir()
        (retry_1 / "TEST-emulator.xml").write_text(
            "<testsuite><testcase name=\"loadsPage\" "
            "classname=\"dev.sk2andy.materialbrowser.browser.gecko.GeckoTest\" time=\"5.0\">"
            "<failure message=\"again\">again</failure></testcase></testsuite>",
            encoding="utf-8",
        )
        lines, status = render(collect(self.shards), set())
        text = "\n".join(lines)

        self.assertEqual(1, status)
        self.assertIn("| 6 | 2 | 1 | 1 | 0 | 2 | 0 min 13 s |", text)
        self.assertIn("Flaky — failed, then passed on the rerun (1)", text)
        self.assertIn("- `TabsTest#closesTab` — Expected &lt;1&gt; tabs", text)
        # The test that failed twice keeps its first failure's details.
        self.assertIn("<code>GeckoTest#loadsPage</code> — java.lang.IllegalStateException", text)

    def test_new_failures_lists_only_unlisted_failing_tests(self):
        baseline_file = self.root / "baseline.txt"
        baseline_file.write_text("dev.sk2andy.materialbrowser.ui.TabsTest#closesTab\n", encoding="utf-8")
        output = self.root / "retry.txt"

        main(self.shards + ["--baseline", str(baseline_file), "--new-failures", str(output)])

        self.assertEqual(
            "dev.sk2andy.materialbrowser.browser.gecko.GeckoTest#loadsPage\n",
            output.read_text(encoding="utf-8"),
        )

    def test_no_results_at_all_fails(self):
        empty = self.root / "nothing"
        empty.mkdir()
        _, status = render(collect([str(empty)]), set())

        self.assertEqual(1, status)


if __name__ == "__main__":
    unittest.main()
