#!/usr/bin/env python3
"""Tests for compile_material_symbols.py: path normalization and up-to-date outputs."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import compile_material_symbols as symbols  # noqa: E402

SYMBOL_GRID = "0 -960 960 960"
DP_GRID = "0 0 24 24"


def normalize(path, view_box=SYMBOL_GRID):
    return symbols.normalize({"viewBox": view_box, "d": path})


class NormalizeTest(unittest.TestCase):
    def test_absolute_y_moves_into_the_positive_viewport(self):
        self.assertEqual(normalize("M480-424 284-228H200V-40Z"), "M480 536 L284 732 H200 V920 Z")

    def test_relative_commands_keep_their_offsets(self):
        self.assertEqual(normalize("M100-100q-11 11-28 11t-28-11z"), "M100 860 q-11 11 -28 11 t-28 -11 Z")

    def test_leading_relative_moveto_is_absolute_and_continues_relative(self):
        self.assertEqual(normalize("m382-354 339-339"), "M382 606 l339 -339")

    def test_compact_decimals_split_into_separate_numbers(self):
        self.assertEqual(normalize("M0-10l.5.5-1.25.75"), "M0 950 l0.5 0.5 l-1.25 0.75")

    def test_arc_flags_may_be_written_without_separators(self):
        self.assertEqual(normalize("M10-10a5 5 0 01 10 0"), "M10 950 a5 5 0 0 1 10 0")

    def test_dp_grid_scales_lengths_but_not_arc_angles_or_flags(self):
        self.assertEqual(
            normalize("M12 2a10 10 45 1 0 1 1z", DP_GRID),
            "M480 80 a400 400 45 1 0 40 40 Z",
        )

    def test_unknown_view_box_is_rejected(self):
        with self.assertRaises(ValueError):
            normalize("M0 0", "0 0 48 48")


class OutputsTest(unittest.TestCase):
    def test_every_symbol_is_cached(self):
        cache = symbols.load_cache()
        missing = [key for key in symbols.required_keys() if key not in cache]
        self.assertEqual([], missing, "run: scripts/compile_material_symbols.py fetch")
        unused = sorted(set(cache) - set(symbols.required_keys()))
        self.assertEqual([], unused, "run: scripts/compile_material_symbols.py fetch")

    def test_committed_outputs_are_up_to_date(self):
        for path, text in symbols.outputs(symbols.load_cache()).items():
            with self.subTest(path=path.name):
                self.assertTrue(path.exists(), f"{path} is missing")
                self.assertEqual(text, path.read_text(), f"{path.name} is stale")

    def test_names_are_unique(self):
        drawables = [entry[0] for entry in symbols.DRAWABLES]
        icons = [entry[0] for entry in symbols.COMPOSE_ICONS]
        self.assertEqual(len(drawables), len(set(drawables)))
        self.assertEqual(len(icons), len(set(icons)))

    def test_filled_compose_icons_say_so_in_their_name(self):
        for name, _, filled, _ in symbols.COMPOSE_ICONS:
            self.assertEqual(filled, name.endswith("Filled"), name)


if __name__ == "__main__":
    unittest.main()
