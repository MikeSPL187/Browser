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

    def test_an_aborted_run_keeps_the_sites_it_checked(self):
        text = smoke.report([smoke.SiteResult("VK", "https://vk.com/", alive=True)], "x",
                            aborted="YouTube: TimeoutExpired: adb")
        self.assertIn(f"⚠️ прогон прерван: проверено 1 из {len(smoke.SITES)}", text)
        self.assertIn("Прерван: `YouTube: TimeoutExpired: adb`", text)
        self.assertIn("| [VK](https://vk.com/) | ✅ жив |", text)

    def test_a_hung_adb_command_is_a_stuck_emulator_not_a_crash(self):
        real = smoke.subprocess.run
        def hang(*args, **kwargs):
            raise smoke.subprocess.TimeoutExpired(args[0], kwargs.get("timeout"))
        smoke.subprocess.run = hang
        try:
            with self.assertRaisesRegex(smoke.EmulatorStuck, "adb shell pidof x: no answer"):
                smoke.adb("shell", "pidof", "x")
        finally:
            smoke.subprocess.run = real

    def test_a_lost_device_is_a_stuck_emulator_not_an_empty_answer(self):
        real = smoke.subprocess.run
        smoke.subprocess.run = lambda *args, **kwargs: smoke.subprocess.CompletedProcess(
            args[0], 1, stdout="", stderr="adb: device offline\n")
        try:
            with self.assertRaisesRegex(smoke.EmulatorStuck, "adb shell pidof x: adb: device offline"):
                smoke.adb("shell", "pidof", "x")
        finally:
            smoke.subprocess.run = real

    def test_a_missing_process_is_still_an_empty_answer(self):
        real = smoke.subprocess.run
        smoke.subprocess.run = lambda *args, **kwargs: smoke.subprocess.CompletedProcess(
            args[0], 1, stdout="", stderr="")
        try:
            self.assertEqual("", smoke.adb("shell", "pidof", "x"))
        finally:
            smoke.subprocess.run = real

    def run_with_stalls(self, stalling, recovers=True):
        def check(package, name, url, settle):
            if name in stalling:
                raise smoke.EmulatorStuck(f"adb shell am start {name}: no answer in 120 s")
            return smoke.SiteResult(name, url, alive=True)
        real = smoke.stuck_diagnostics
        smoke.stuck_diagnostics = lambda: None
        try:
            return smoke.run_sites("x", 0, check=check, recover_emulator=lambda: recovers)
        finally:
            smoke.stuck_diagnostics = real

    def test_a_hang_marks_the_site_unchecked_and_the_run_goes_on(self):
        results, aborted = self.run_with_stalls({"Сбербанк Онлайн"})
        self.assertEqual("", aborted)
        self.assertEqual(len(smoke.SITES), len(results))
        sber = next(result for result in results if result.name == "Сбербанк Онлайн")
        self.assertTrue(sber.stalled)
        self.assertFalse(sber.crashed)
        self.assertFalse(sber.unopened)
        text = smoke.report(results, "x")
        self.assertIn("✅ без вылетов · ⏸ эмулятор зависал: 1", text)
        self.assertIn("| [Сбербанк Онлайн](https://online.sberbank.ru/) | ⏸ эмулятор завис, сайт не проверен |", text)
        self.assertIn("⏸ Сбербанк Онлайн: `adb shell am start Сбербанк Онлайн: no answer in 120 s`", text)

    def test_an_emulator_that_does_not_come_back_stops_the_run(self):
        results, aborted = self.run_with_stalls({"Т-Банк"}, recovers=False)
        self.assertEqual(7, len(results))
        self.assertTrue(aborted.startswith("Т-Банк: эмулятор перестал отвечать"))

    def test_hangs_that_keep_coming_back_stop_the_run(self):
        stalling = {name for name, _ in smoke.SITES[6:6 + smoke.MAX_STALLS + 1]}
        results, aborted = self.run_with_stalls(stalling)
        self.assertEqual(6 + smoke.MAX_STALLS, len(results))
        self.assertIn("эмулятор перестал отвечать", aborted)

    def test_a_system_server_that_died_is_a_stuck_emulator(self):
        self.assertEqual("", smoke.system_restart("612", "612"))
        self.assertEqual("", smoke.system_restart("", "700"))
        self.assertEqual("system_server перезапустился (pid 612 → 7569)", smoke.system_restart("612", "7569"))
        self.assertEqual("system_server перезапустился (pid 612 → нет)", smoke.system_restart("612", ""))

    def test_a_site_during_which_the_system_restarted_is_not_a_crash(self):
        answers = {"pidof system_server": iter(["612", "7569"])}
        def adb(*args, **kwargs):
            command = " ".join(args[-2:])
            if command in answers:
                return next(answers[command])
            return ""
        real_adb, real_sleep = smoke.adb, smoke.time.sleep
        smoke.adb, smoke.time.sleep = adb, lambda seconds: None
        try:
            with self.assertRaisesRegex(smoke.EmulatorStuck, "system_server перезапустился"):
                smoke.check_site("x", "Т-Банк", "https://www.tbank.ru/login/", 0)
        finally:
            smoke.adb, smoke.time.sleep = real_adb, real_sleep

    def test_a_quiet_death_keeps_why_the_process_ended(self):
        dead = smoke.SiteResult("Т-Банк", "https://www.tbank.ru/login/", alive=False,
                                exit_info="ApplicationExitInfo #0:\nreason=3 (LOW_MEMORY)",
                                start_output="Status: ok",
                                system_log="I lowmemorykiller: Kill 'io.github.mikespl187.vola.preview'")
        self.assertTrue(dead.crashed)
        self.assertTrue(dead.quiet_death)
        text = smoke.report([dead], "x")
        self.assertIn("Процесс завершился без журнала сбоев", text)
        self.assertIn("reason=3 (LOW_MEMORY)", text)
        self.assertIn("lowmemorykiller", text)

    def test_a_crash_with_a_log_is_not_a_quiet_death(self):
        crash = smoke.SiteResult("VK", "https://vk.com/", alive=False, crash_log="FATAL EXCEPTION: main")
        self.assertFalse(crash.quiet_death)
        self.assertNotIn("без журнала сбоев", smoke.report([crash], "x"))

    def test_exit_reasons_keep_a_few_lines_of_each_record(self):
        dump = """ACTIVITY MANAGER PROCESS EXIT INFO (dumpsys activity exit-info)
  package: io.github.mikespl187.vola.preview
    Historical Process Exit for uid=10190
        ApplicationExitInfo #0:
          timestamp=2026-10-13 15:30:01.000 pid=4242 realUid=10190 packageUid=10190 definingUid=10190 user=0
          process=io.github.mikespl187.vola.preview reason=3 (LOW_MEMORY) subreason=0 (UNKNOWN) status=0
          importance=100 pss=512MB rss=600MB description=null state=empty trace=null
        ApplicationExitInfo #1:
          timestamp=2026-10-13 15:29:00.000 pid=4100
          process=io.github.mikespl187.vola.preview reason=10 (USER_REQUESTED) subreason=23 (FORCE_STOP)
"""
        reasons = smoke.exit_reasons(dump)
        self.assertTrue(reasons.startswith("ApplicationExitInfo #0:"))
        self.assertIn("reason=3 (LOW_MEMORY)", reasons)
        self.assertIn("FORCE_STOP", reasons)
        self.assertNotIn("Historical Process Exit", reasons)

    def test_the_system_log_keeps_process_deaths_and_the_package(self):
        log = "\n".join([
            "I chatty: noise",
            "I ActivityManager: Process io.github.mikespl187.vola.preview (pid 4242) has died: fg TOP",
            "W Unrelated: noise",
            "E GeckoRuntime: Parent process crashed",
        ])
        tail = smoke.system_log_tail(log, "io.github.mikespl187.vola.preview")
        self.assertIn("has died", tail)
        self.assertIn("GeckoRuntime", tail)
        self.assertNotIn("chatty", tail)

    def test_every_site_is_an_https_page(self):
        self.assertTrue(all(url.startswith("https://") for _, url in smoke.SITES))
        self.assertEqual(len({url for _, url in smoke.SITES}), len(smoke.SITES))


if __name__ == "__main__":
    unittest.main()
