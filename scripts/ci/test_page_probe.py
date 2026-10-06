#!/usr/bin/env python3
"""Tests for the page probe helpers of the emulator tour."""

import struct
import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))

import page_probe  # noqa: E402


def screencap(width, rows, header=16):
    """A raw screencap whose every row has one color, from [(count, (r, g, b)), …]."""
    height = sum(count for count, _ in rows)
    data = bytearray(struct.pack("<III", width, height, 1))
    if header == 16:
        data += struct.pack("<I", 0)
    for count, (red, green, blue) in rows:
        data += bytes((red, green, blue, 255)) * width * count
    return bytes(data)


def band_color(index):
    return (page_probe.BAND_RED_BASE + index * page_probe.BAND_RED_STEP,
            page_probe.BAND_GREEN, page_probe.BAND_BLUE)


class PageProbeTest(unittest.TestCase):
    def test_screencap_headers_of_old_and_new_android(self):
        for header in (12, 16):
            width, height, pixels = page_probe.parse_raw_screencap(
                screencap(2, [(3, (1, 2, 3))], header=header),
            )
            self.assertEqual((2, 3), (width, height))
            self.assertEqual((1, 2, 3), tuple(pixels[:3]))

    def test_a_malformed_screencap_is_refused(self):
        with self.assertRaises(ValueError):
            page_probe.parse_raw_screencap(b"\x00" * 8)
        with self.assertRaises(ValueError):
            page_probe.parse_raw_screencap(struct.pack("<III", 10, 10, 1) + b"\x00" * 7)

    def test_band_colors_decode_with_tolerance(self):
        self.assertEqual(7, page_probe.band_at(*band_color(7)))
        red, green, blue = band_color(12)
        self.assertEqual(12, page_probe.band_at(red + 2, green - 2, blue + 1))
        self.assertIsNone(page_probe.band_at(250, 250, 250))
        self.assertIsNone(page_probe.band_at(red, green + 10, blue))
        self.assertIsNone(page_probe.band_at(red + page_probe.BAND_RED_STEP // 2, green, blue))

    def test_runs_and_target_skip_bands_cut_by_the_screen_edges(self):
        rows = [(50, (0, 0, 0)), (40, band_color(3)), (100, band_color(4)), (100, band_color(5)),
                (100, band_color(6)), (30, band_color(7)), (80, (0, 0, 0))]
        width, height, pixels = page_probe.parse_raw_screencap(screencap(4, rows))
        runs = page_probe.band_runs(width, height, pixels, x=1)
        self.assertEqual({3: (50, 89), 4: (90, 189), 5: (190, 289), 6: (290, 389), 7: (390, 419)},
                         runs)
        self.assertEqual((5, 190, 289), page_probe.target_band(runs, height))
        self.assertIsNone(page_probe.target_band({1: (0, 99)}, 200))

    def test_page_result_is_parsed(self):
        self.assertEqual((5, 120, 252), page_probe.parse_result("PROBE tap 5 y 120 of 252"))
        self.assertIsNone(page_probe.parse_result("PROBE ready"))
        self.assertIsNone(page_probe.parse_result(None))

    def test_offset_is_zero_when_the_page_got_the_finger(self):
        self.assertEqual(0, page_probe.touch_offset(240, 5, 190, 289, (5, 50, 100)))

    def test_offset_measures_a_tap_that_landed_on_another_band(self):
        # The finger is in the middle of band 5; the page got band 4, 30 px from its top: the
        # page placed the tap 120 px higher than the finger.
        self.assertEqual(-120, page_probe.touch_offset(240, 5, 190, 289, (4, 30, 100)))

    def test_offset_is_measured_in_page_pixels_under_zoom(self):
        # Bands drawn twice as tall as the page lays them out (zoomed in).
        self.assertEqual(0, page_probe.touch_offset(300, 5, 200, 399, (5, 50, 100)))
        self.assertIsNone(page_probe.touch_offset(300, 5, 200, 399, None))

    def test_gfxinfo_summary_is_parsed(self):
        text = """Applications Graphics Acceleration Info:
Stats since: 1ns
Total frames rendered: 120
Janky frames: 18 (15.00%)
Janky frames (legacy): 20 (16.67%)
50th percentile: 9ms
90th percentile: 21ms
95th percentile: 30ms
99th percentile: 48ms
"""
        stats = page_probe.parse_gfxinfo(text)
        self.assertEqual({"frames": 120, "janky": 18, "p50": 9, "p90": 21, "p95": 30, "p99": 48},
                         stats)
        self.assertEqual("120 frames, 18 janky (15%), p50 9 ms, p90 21 ms, p99 48 ms",
                         page_probe.describe_frames(stats))
        self.assertEqual("no frame statistics", page_probe.describe_frames({}))

    def test_card_spread_tells_an_empty_card_from_a_page(self):
        empty = page_probe.parse_raw_screencap(screencap(200, [(400, (250, 250, 250))]))
        self.assertEqual(0.0, page_probe.card_spread(*empty))
        striped = [(8, (250, 250, 250)) if index % 2 == 0 else (8, (20, 20, 20))
                   for index in range(50)]
        page = page_probe.parse_raw_screencap(screencap(200, striped))
        self.assertGreater(page_probe.card_spread(*page, step=4), 50)


if __name__ == "__main__":
    unittest.main()
