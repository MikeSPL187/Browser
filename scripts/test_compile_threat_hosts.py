#!/usr/bin/env python3

import hashlib
import tempfile
import unittest
from pathlib import Path

from compile_hagezi_hosts import compile_source, is_host
from compile_threat_hosts import write_asset


PROJECT_DIR = Path(__file__).resolve().parents[1]
PINNED_ASSET = PROJECT_DIR / "app/src/main/assets/threat_hosts.txt"
PINNED_REVISION = "b3f10c70552d10259d966e39402ac73ec468d47f"
PINNED_SHA256 = "96c56bf35e00df06a6198a7f62abe2d6f357dfa64a5041846c1303099b22496d"


def fixture(*rules: str) -> bytes:
    return (
        "[Adblock Plus]\n"
        "! Title: Fixture\n"
        f"! Number of entries: {len(rules)}\n"
        + "".join(f"{rule}\n" for rule in rules)
    ).encode()


class ThreatHostCompilerTest(unittest.TestCase):
    def test_output_is_deterministic_and_records_provenance(self):
        source = fixture("||login.b.example^", "||a.example^")
        compiled = compile_source(source, hashlib.sha256(source).hexdigest())
        with tempfile.TemporaryDirectory() as directory:
            first = Path(directory) / "first.txt"
            second = Path(directory) / "second.txt"
            write_asset(first, "revision", compiled)
            write_asset(second, "revision", compiled)

            self.assertEqual(first.read_bytes(), second.read_bytes())
            lines = first.read_text().splitlines()
            self.assertIn("# Source revision: revision", lines)
            self.assertIn(f"# Source SHA-256: {hashlib.sha256(source).hexdigest()}", lines)
            self.assertIn("# Hosts: 2", lines)
            self.assertEqual(["a.example", "login.b.example"], lines[-2:])

    def test_checked_in_asset_is_bounded_sorted_unique_and_pinned(self):
        lines = PINNED_ASSET.read_text(encoding="utf-8").splitlines()
        hosts = [line for line in lines if line and not line.startswith("#")]

        self.assertIn(f"# Source revision: {PINNED_REVISION}", lines)
        self.assertIn(f"# Source SHA-256: {PINNED_SHA256}", lines)
        self.assertIn(f"# Hosts: {len(hosts)}", lines)
        self.assertEqual(203_211, len(hosts))
        self.assertEqual(sorted(hosts), hosts)
        self.assertEqual(len(set(hosts)), len(hosts))
        self.assertTrue(all(is_host(host) for host in hosts))
        self.assertLessEqual(PINNED_ASSET.stat().st_size, 4_800_000)

    def test_checked_in_asset_spares_sites_people_use_every_day(self):
        hosts = set(
            line
            for line in PINNED_ASSET.read_text(encoding="utf-8").splitlines()
            if line and not line.startswith("#")
        )
        for host in (
            "google.com", "youtube.com", "github.com", "github.io", "web.app", "pages.dev",
            "vercel.app", "netlify.app", "blogspot.com", "wikipedia.org", "yandex.ru", "vk.com",
            "mail.ru", "telegram.org", "t.me", "bit.ly", "drive.google.com", "docs.google.com",
        ):
            self.assertNotIn(host, hosts)


if __name__ == "__main__":
    unittest.main()
