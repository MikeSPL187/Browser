#!/usr/bin/env python3

import tempfile
import unittest
from pathlib import Path

from compile_network_filters import best_token, compile_sources, write_asset


PROJECT_DIR = Path(__file__).resolve().parents[1]
PINNED_ASSET = PROJECT_DIR / "app/src/blocking/gecko/easylist_network_filters.txt"


class NetworkFilterCompilerTest(unittest.TestCase):
    def compile(self, source: str):
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "filters.txt"
            path.write_text(source, encoding="utf-8")
            return compile_sources([path])

    def test_keeps_url_rules_with_types_party_and_domains(self):
        records, stats = self.compile(
            "/ads/banner.\n"
            "||cdn.example^/pixel.gif$image,third-party\n"
            "-ad-300x250.$domain=news.example|~sport.news.example\n"
            "@@||cdn.example/ads/player.js$script,domain=video.example\n"
            "||tracker.example^$third-party\n"
        )
        by_pattern = {record.pattern: record for record in records}
        self.assertEqual(4, len(records))
        self.assertEqual(1, stats.host_list_rules)
        self.assertEqual(("image", "imageset"), by_pattern["||cdn.example^/pixel.gif"].types)
        self.assertEqual("3", by_pattern["||cdn.example^/pixel.gif"].party)
        scoped = by_pattern["-ad-300x250."]
        self.assertEqual(("news.example",), scoped.page_hosts)
        self.assertEqual(("sport.news.example",), scoped.excluded_page_hosts)
        self.assertEqual("A", by_pattern["||cdn.example/ads/player.js"].action)

    def test_leaves_out_rules_that_change_what_a_match_does(self):
        records, _ = self.compile(
            "||ads.example/script.js$redirect=noopjs\n"
            "/banner/$important\n"
            "||ads.example^$csp=script-src 'none'\n"
            "/^https?:\\/\\/ads\\./\n"
            "@@||news.example^$document\n"
            "/ads/top.js$match-case\n"
            "/popup/$popup\n"
        )
        self.assertEqual([], records)

    def test_option_values_with_slashes_stay_options(self):
        records, _ = self.compile(
            "||tracker.example^$removeparam=/^utm_/\n"
            "/ads/a$b/c.js\n"
        )
        # `removeparam` changes what a match does, so the rule is left out rather than turned into
        # URL text; a `$` inside the path is never mistaken for options either.
        self.assertEqual([], [record for record in records if "removeparam" in record.pattern])
        self.assertEqual([], [record for record in records if "$" in record.pattern])

    def test_negated_types_cover_every_other_type(self):
        records, _ = self.compile("/track/event$~image,~script\n")
        self.assertNotIn("image", records[0].types)
        self.assertNotIn("script", records[0].types)
        self.assertIn("xmlhttprequest", records[0].types)

    def test_token_is_one_the_url_must_contain_whole(self):
        self.assertEqual("banner", best_token("/ads/banner."))
        self.assertEqual("ads", best_token("/ads/banner*"))
        self.assertEqual("example", best_token("||cdn.example^/pixel.gif"))
        self.assertEqual("", best_token("*banner*"))
        self.assertEqual("analytics", best_token("|https://analytics."))

    def test_rules_without_any_token_need_a_page_scope(self):
        records, stats = self.compile("/ab$script,domain=news.example\n/ab*\n")
        self.assertEqual(1, len(records))
        self.assertEqual("-", records[0].token)
        self.assertEqual(1, stats.tokenless_rules)

    def test_badfilter_disables_its_rule(self):
        records, _ = self.compile("/ads/banner.$image\n/ads/banner.$image,badfilter\n")
        self.assertEqual([], records)

    def test_written_asset_declares_its_counts(self):
        records, stats = self.compile("/ads/banner.\n@@/ads/banner.$domain=ok.example\n")
        with tempfile.TemporaryDirectory() as directory:
            output = Path(directory) / "filters.txt"
            write_asset(output, "rev", records, stats)
            text = output.read_text(encoding="utf-8")
        self.assertTrue(text.startswith("candy-network-filter:2\n"))
        self.assertIn("# Rules: 2\n", text)
        self.assertIn("# Allow rules: 1\n", text)

    def test_pinned_asset_is_a_complete_compiled_list(self):
        text = PINNED_ASSET.read_text(encoding="utf-8")
        rows = [line for line in text.splitlines()[1:] if line and not line.startswith("#")]
        declared = int(next(line for line in text.splitlines() if line.startswith("# Rules:")).split(":")[1])
        self.assertTrue(text.startswith("candy-network-filter:2\n"))
        self.assertEqual(declared, len(rows))
        self.assertGreater(len(rows), 10_000)
        self.assertTrue(all(len(row.split("\t")) == 7 for row in rows))


if __name__ == "__main__":
    unittest.main()
