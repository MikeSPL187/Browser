#!/usr/bin/env python3
"""Tests for ci/a11y_audit.py: touch size, labels, skipped subtrees and the baseline."""

import io
import sys
import tempfile
import unittest
from contextlib import redirect_stdout
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "ci"))
import a11y_audit as audit  # noqa: E402

PACKAGE = "io.github.mikespl187.vola.debug"
# Density 320: 2 px per dp, so 96 px is exactly 48 dp.
DUMP = f"""<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<hierarchy rotation="0">
  <node class="android.widget.FrameLayout" package="{PACKAGE}" clickable="false" bounds="[0,0][1080,2400]">
    <node class="android.view.View" package="{PACKAGE}" content-desc="More options" clickable="true" bounds="[0,0][96,96]" />
    <node class="android.view.View" package="{PACKAGE}" content-desc="Close" clickable="true" bounds="[0,100][64,164]" />
    <node class="android.view.View" package="{PACKAGE}" resource-id="{PACKAGE}:id/star" clickable="true" bounds="[0,200][96,296]" />
    <node class="android.view.View" package="{PACKAGE}" clickable="true" bounds="[0,300][400,396]">
      <node class="android.widget.TextView" package="{PACKAGE}" text="Settings" clickable="false" bounds="[10,310][200,380]" />
    </node>
    <node class="android.view.View" package="{PACKAGE}" content-desc="Hidden" clickable="true" visible-to-user="false" bounds="[0,400][10,410]" />
    <node class="android.view.View" package="{PACKAGE}" content-desc="Peeking card" clickable="true" bounds="[0,420][40,900]" />
    <node class="androidx.compose.ui.platform.ComposeView" package="{PACKAGE}" scrollable="true" clickable="false" bounds="[200,1000][800,1200]">
      <node class="android.view.View" package="{PACKAGE}" content-desc="Cut row" clickable="true" bounds="[200,1150][800,1200]" />
    </node>
    <node class="org.mozilla.geckoview.GeckoView" package="{PACKAGE}" clickable="false" bounds="[0,500][1080,2000]">
      <node class="android.view.View" package="{PACKAGE}" clickable="true" bounds="[0,600][20,620]" />
    </node>
  </node>
  <node class="android.widget.FrameLayout" package="com.android.systemui" clickable="true" bounds="[0,0][10,10]" />
</hierarchy>
"""


class AuditTest(unittest.TestCase):
    def test_reports_small_and_unlabeled_vola_controls_only(self):
        self.assertEqual(
            audit.audit(DUMP, PACKAGE, density=320),
            [("small", "Close", 32, 32), ("unlabeled", "star", 48, 48)],
        )

    def test_a_list_item_scrolled_past_the_edge_is_cut_not_small_or_unlabeled(self):
        dump = f"""<hierarchy rotation="0">
  <node class="android.view.View" package="{PACKAGE}" scrollable="true" clickable="false" bounds="[0,500][1080,2000]">
    <node class="android.view.View" package="{PACKAGE}" clickable="true" bounds="[40,420][520,560]">
      <node class="android.view.View" package="{PACKAGE}" content-desc="Mail, mail.example.com" clickable="false" bounds="[40,500][520,560]" />
    </node>
    <node class="android.view.View" package="{PACKAGE}" clickable="true" bounds="[40,1960][520,2000]">
      <node class="android.view.View" package="{PACKAGE}" content-desc="Close" clickable="true" bounds="[400,1962][520,2090]" />
    </node>
    <node class="android.view.View" package="{PACKAGE}" clickable="true" bounds="[600,1960][1040,2000]" />
    <node class="android.view.View" package="{PACKAGE}" clickable="true" bounds="[40,1000][520,1400]" />
  </node>
</hierarchy>
"""
        # Only what reaches past the edge is excused; an item that merely touches it still needs a label.
        self.assertEqual(
            audit.audit(dump, PACKAGE, density=320),
            [("unlabeled", "View", 220, 20), ("unlabeled", "View", 240, 200)],
        )

    def test_broken_dump_has_no_findings(self):
        self.assertEqual(audit.audit("ERROR: could not get idle state.", PACKAGE, 420), [])

    def test_small_key_ignores_the_screen_unlabeled_key_ignores_the_size(self):
        self.assertEqual(audit.finding_key("small", "Close", 32, 32, "find-dark"), "small | Close | 32x32 dp")
        self.assertEqual(
            audit.finding_key("unlabeled", "View", 42, 675, "tab-overview-ru"),
            "unlabeled | View | tab-overview",
        )


class BaselineTest(unittest.TestCase):
    def run_audit(self, dumps, baseline, *extra):
        with redirect_stdout(io.StringIO()) as output:
            status = audit.main([str(dumps), PACKAGE, "--baseline", str(baseline), *extra])
        return status, output.getvalue()

    def test_collects_without_baseline_then_enforces_only_new_findings(self):
        with tempfile.TemporaryDirectory() as temp:
            dumps = Path(temp) / "ui-dumps"
            dumps.mkdir()
            (dumps / "density.txt").write_text("320\n")
            (dumps / "05-find-light.xml").write_text(DUMP)
            baseline = Path(temp) / "a11y-baseline.txt"

            status, output = self.run_audit(dumps, baseline)
            self.assertEqual(status, 0)
            self.assertIn("not enforced", output)

            self.assertEqual(self.run_audit(dumps, baseline, "--write-baseline")[0], 0)
            self.assertEqual(self.run_audit(dumps, baseline)[0], 0)

            baseline.write_text("small | Close | 32x32 dp\n")
            status, output = self.run_audit(dumps, baseline)
            self.assertEqual(status, 1)
            self.assertIn("New accessibility finding: unlabeled | star | find (screens: find-light)", output)


if __name__ == "__main__":
    unittest.main()
