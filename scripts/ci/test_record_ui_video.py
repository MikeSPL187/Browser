#!/usr/bin/env python3
"""Tests for record_ui_video.py: the commands it sends, the parsing and the job summary."""

import sys
import tempfile
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import record_ui_video as video  # noqa: E402

DUMP = b"""<?xml version='1.0' encoding='UTF-8' standalone='yes' ?>
<hierarchy rotation="0">
  <node text="" content-desc="" bounds="[0,0][1080,2400]">
    <node text="localhost:8765/touch.html" content-desc="" bounds="[100,2200][800,2340]" />
    <node text="" content-desc="Other options" bounds="[900,100][1000,200]" />
    <node text="" content-desc="More options" bounds="[900,2210][1020,2330]" />
    <node text="broken" content-desc="" bounds="" />
  </node>
</hierarchy>"""


class CommandsTest(unittest.TestCase):
    def test_screenrecord_records_at_the_planned_size_and_rate(self):
        command = video.screenrecord_command(video.remote_path("scroll"))
        self.assertEqual(["adb", "shell", "screenrecord", "--bit-rate", "6000000", "--size",
                          "720x1600", "--time-limit", "90", "/sdcard/vola-scroll.mp4"], command)

    def test_without_a_size_the_native_size_is_kept(self):
        command = video.screenrecord_command("/sdcard/x.mp4", size=None)
        self.assertNotIn("--size", command)
        self.assertLessEqual(video.TIME_LIMIT_S, 180)

    def test_a_fling_down_moves_the_finger_up_quickly(self):
        args = video.fling(1080, 2400, down=True)
        self.assertEqual(["shell", "input", "swipe", "540", "1800", "540", "720", "150"], args)
        self.assertEqual(["shell", "input", "swipe", "540", "720", "540", "1800", "150"],
                         video.fling(1080, 2400, down=False))

    def test_the_bar_swipe_stays_on_the_bar(self):
        self.assertEqual(["shell", "input", "swipe", "777", "2270", "302", "2270", "220"],
                         video.bar_swipe(1080, 2270, to_left=True))
        self.assertEqual(["shell", "input", "swipe", "302", "2270", "777", "2270", "220"],
                         video.bar_swipe(1080, 2270, to_left=False))

    def test_the_overview_swipe_goes_up_from_the_bar(self):
        self.assertEqual(["shell", "input", "swipe", "540", "2270", "540", "840", "350"],
                         video.overview_swipe(1080, 2400, 2270))


class ParsingTest(unittest.TestCase):
    def test_screen_size_prefers_an_override(self):
        self.assertEqual((1080, 2400), video.parse_screen_size("Physical size: 1080x2400\n"))
        self.assertEqual((720, 1600), video.parse_screen_size(
            "Physical size: 1080x2400\nOverride size: 720x1600\n"))
        self.assertEqual(video.DEFAULT_SCREEN, video.parse_screen_size("error: no devices"))

    def test_nodes_and_lookup(self):
        nodes = video.parse_nodes(DUMP)
        self.assertEqual(4, len(nodes))
        self.assertEqual((960, 2270), video.find_node(nodes, video.MENU_LABELS)["center"])
        self.assertEqual((450, 2270),
                         video.find_node(nodes, ("touch.html",), contains=True)["center"])
        self.assertIsNone(video.find_node(nodes, ("touch.html",)))
        self.assertEqual([], video.parse_nodes(b"ERROR: could not get idle state"))

    def test_the_bar_is_found_by_its_menu_or_assumed_at_the_bottom(self):
        self.assertEqual(2270, video.bar_y(video.parse_nodes(DUMP), 2400))
        self.assertEqual(2280, video.bar_y([], 2400))
        top_menu = [{"text": "", "desc": "More options", "center": (960, 150)}]
        self.assertEqual(2280, video.bar_y(top_menu, 2400))

    def test_launcher_component_and_total_time(self):
        resolved = "priority=0 preferredOrder=0\nio.github.mikespl187.vola.debug/dev.Main\n"
        self.assertEqual("io.github.mikespl187.vola.debug/dev.Main",
                         video.launcher_component(resolved))
        self.assertIsNone(video.launcher_component("No activity found\n"))
        self.assertIsNone(video.launcher_component(""))
        self.assertEqual(812, video.parse_total_time("Status: ok\nTotalTime: 812\nWaitTime: 830"))
        self.assertIsNone(video.parse_total_time("Error: Activity not started"))

    def test_duration_from_ffprobe(self):
        self.assertEqual(12.48, video.parse_duration("12.480000\n"))
        self.assertIsNone(video.parse_duration("N/A\n"))
        self.assertIsNone(video.parse_duration(""))
        self.assertIsNone(video.parse_duration("0.000000"))

    def test_scroll_page_falls_back_to_the_touch_probe(self):
        with tempfile.TemporaryDirectory() as pages:
            self.assertEqual("touch.html", video.scroll_page(pages))
            (Path(pages) / "scroll.html").write_text("<!doctype html>")
            self.assertEqual("scroll.html", video.scroll_page(pages))


class ContactSheetTest(unittest.TestCase):
    def test_eight_moments_spread_over_the_video(self):
        times = video.sample_times(16.0)
        self.assertEqual(8, len(times))
        self.assertEqual(1.0, times[0])
        self.assertEqual(15.0, times[-1])
        self.assertEqual([], video.sample_times(None))
        self.assertEqual([], video.sample_times(0))

    def test_frame_and_tile_commands(self):
        frame = video.frame_command(Path("a.mp4"), 1.5, Path("f-1.png"))
        self.assertEqual(["-ss", "1.500", "-i", "a.mp4"], frame[4:8])
        self.assertEqual(["-frames:v", "1", "f-1.png"], frame[-3:])
        tile = video.tile_command("frame-%d.png", "sheet.png")
        self.assertIn("scale=240:-2,tile=4x2:padding=8:margin=8:color=gray", tile)
        self.assertEqual("sheet.png", tile[-1])


class SummaryTest(unittest.TestCase):
    def test_summary_lists_scenarios_and_the_checklist(self):
        results = [
            {"title": "Холодный старт", "duration": 8.24,
             "files": ["cold-start.mp4", "cold-start-frames.png"], "note": "am start -W: 812 мс"},
            {"title": "Обзор вкладок", "files": [], "note": "screenrecord не запустился"},
        ]
        summary = video.summary_markdown(results, run_number="42")
        self.assertIn("запуск 42", summary)
        self.assertIn("`ui-video-42`", summary)
        self.assertIn("| Холодный старт | 8.2 с | `cold-start.mp4`, `cold-start-frames.png` | "
                      "am start -W: 812 мс |", summary)
        self.assertIn("| Обзор вкладок | — | не записано | screenrecord не запустился |", summary)
        self.assertEqual(len(video.SCENARIOS) + 1, summary.count("- [ ] "))

    def test_every_scenario_has_a_unique_id(self):
        ids = [scenario for scenario, _, _ in video.SCENARIOS]
        self.assertEqual(len(ids), len(set(ids)))
        self.assertEqual(["cold-start", "scroll", "tab-swipe", "overview"], ids)


if __name__ == "__main__":
    unittest.main()
