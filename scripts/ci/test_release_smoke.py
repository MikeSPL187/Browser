#!/usr/bin/env python3
"""Tests for release_smoke.py: verdicts and the report posted to the nightly issue."""

import sys
import unittest
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import release_smoke as smoke  # noqa: E402


class ReportTest(unittest.TestCase):
    def test_a_quiet_night_is_one_table(self):
        results = [smoke.SiteResult("Google: вход", "https://accounts.google.com/", alive=True),
                   smoke.SiteResult("VK ID", "https://id.vk.ru/", alive=True)]
        text = smoke.report(results, "abc1234")
        self.assertIn("abc1234 · ✅ без вылетов", text)
        self.assertEqual(text.count("✅ жив"), 2)
        self.assertNotIn("###", text)

    def test_a_native_crash_shows_its_symbolicated_stack_and_log(self):
        crash = smoke.SiteResult("Google: вход", "https://accounts.google.com/", alive=False,
                                 crash_log="F DEBUG : #00 pc 0495dd78  libxul.so (BuildId: b5)\n",
                                 symbolicated=[["#00 libxul.so mozilla::AndroidBridge::GetStaticMethodID+0x38"]])
        text = smoke.report([crash, smoke.SiteResult("VK", "https://vk.com/", alive=True)], "abc1234")
        self.assertIn("❌ вылетов: 1 из 2", text)
        self.assertIn("| [Google: вход](https://accounts.google.com/) | ❌ вылет |", text)
        self.assertIn("GetStaticMethodID+0x38", text)
        self.assertIn("logcat -b crash", text)

    def test_java_crash_and_a_survived_error_are_told_apart(self):
        java = smoke.SiteResult("YouTube", "https://m.youtube.com/", alive=False,
                                crash_log="E AndroidRuntime: FATAL EXCEPTION: main\n")
        survived = smoke.SiteResult("Яндекс", "https://ya.ru/", alive=True,
                                    crash_log="E AndroidRuntime: crash in :tab process\n")
        self.assertTrue(java.java_crash and java.crashed)
        self.assertTrue(survived.crashed)
        text = smoke.report([java, survived], "x")
        self.assertIn("❌ вылет (Java)", text)
        self.assertIn("⚠️ сбой в журнале, процесс жив", text)

    def test_a_page_without_a_content_process_is_flagged_not_failed(self):
        empty = smoke.SiteResult("VK", "https://vk.com/", alive=True, loaded=False)
        self.assertTrue(empty.unopened)
        self.assertFalse(empty.crashed)
        text = smoke.report([empty, smoke.SiteResult("Яндекс", "https://ya.ru/", alive=True)], "x")
        self.assertIn("✅ без вылетов · ⚠️ не открылись: 1", text)
        self.assertIn("| [VK](https://vk.com/) | ⚠️ страница не открылась (нет процесса вкладки) |", text)

    def test_every_site_is_an_https_page(self):
        self.assertTrue(all(url.startswith("https://") for _, url in smoke.SITES))
        self.assertEqual(len({url for _, url in smoke.SITES}), len(smoke.SITES))


if __name__ == "__main__":
    unittest.main()
