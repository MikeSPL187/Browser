#!/usr/bin/env python3
"""Tests for ci/geckoview_latest.py: release ordering, metadata parsing and the catalog edit."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent / "ci"))
import geckoview_latest as gecko  # noqa: E402

METADATA = """<?xml version="1.0" encoding="UTF-8"?>
<metadata>
  <groupId>org.mozilla.geckoview</groupId>
  <artifactId>geckoview</artifactId>
  <versioning>
    <latest>155.0.20260825090000</latest>
    <release>155.0.20260825090000</release>
    <versions>
      <version>155.0.20260825090000</version>
      <version>156.0.20260921121718</version>
      <version>156.0.1.20260925103000</version>
      <version>157.0b3.20260915000000</version>
      <version>not-a-version</version>
    </versions>
  </versioning>
</metadata>
"""

CATALOG = """[versions]
agp = "9.4.1"
# Keep GeckoView on the latest stable release: it carries the engine's security fixes.
geckoview = "156.0.20260921121718"
compose-bom = "2026.09.00"

[libraries]
geckoview = { module = "org.mozilla.geckoview:geckoview", version.ref = "geckoview" }
"""


class ParseVersionTest(unittest.TestCase):
    def test_major_release(self):
        self.assertEqual(gecko.parse_version("156.0.20260921121718"), ((156, 0, 0), 20260921121718))

    def test_dot_release(self):
        self.assertEqual(gecko.parse_version("156.0.1.20260925103000"), ((156, 0, 1), 20260925103000))

    def test_rejects_beta_and_garbage(self):
        for value in ["157.0b3.20260915000000", "156.0", "156.20260921121718", "", "latest"]:
            with self.subTest(value=value):
                self.assertIsNone(gecko.parse_version(value))

    def test_display_drops_the_build_id(self):
        self.assertEqual(gecko.display_version("156.0.1.20260925103000"), "156.0.1")
        self.assertEqual(gecko.display_version("157.0.20260929120000"), "157.0")


class LatestReleaseTest(unittest.TestCase):
    def test_dot_release_beats_its_major_release(self):
        versions = gecko.versions_from_metadata(METADATA)
        self.assertEqual(gecko.latest_release(versions), "156.0.1.20260925103000")

    def test_next_major_beats_every_dot_release(self):
        versions = ["156.0.3.20261001000000", "157.0.20260929120000"]
        self.assertEqual(gecko.latest_release(versions), "157.0.20260929120000")

    def test_respin_with_a_newer_build_id_wins(self):
        versions = ["157.0.20260929120000", "157.0.20260930080000"]
        self.assertEqual(gecko.latest_release(versions), "157.0.20260930080000")

    def test_metadata_without_releases_fails_loudly(self):
        with self.assertRaises(ValueError):
            gecko.latest_release(["not-a-version"])


class CatalogTest(unittest.TestCase):
    def test_reads_the_version_not_the_library_line(self):
        self.assertEqual(gecko.current_version(CATALOG), "156.0.20260921121718")

    def test_update_only_for_a_newer_release(self):
        self.assertTrue(gecko.is_newer("156.0.1.20260925103000", "156.0.20260921121718"))
        self.assertFalse(gecko.is_newer("156.0.20260921121718", "156.0.20260921121718"))
        self.assertFalse(gecko.is_newer("155.0.20260825090000", "156.0.20260921121718"))

    def test_with_version_rewrites_only_the_version_line(self):
        updated = gecko.with_version(CATALOG, "157.0.20260929120000")
        self.assertIn('geckoview = "157.0.20260929120000"', updated)
        self.assertEqual(updated.replace("157.0.20260929120000", "156.0.20260921121718"), CATALOG)

    def test_with_version_rejects_a_non_release(self):
        with self.assertRaises(ValueError):
            gecko.with_version(CATALOG, "157.0b3.20260915000000")

    def test_repository_catalog_holds_a_release_version(self):
        catalog = gecko.CATALOG.read_text(encoding="utf-8")
        self.assertIsNotNone(gecko.parse_version(gecko.current_version(catalog)))


if __name__ == "__main__":
    unittest.main()
