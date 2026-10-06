#!/usr/bin/env python3
"""Nightly smoke test of the release build on an emulator (.github/workflows/nightly-smoke.yml).

Opens popular sites one by one in a fresh process - Google and VK sign-in, YouTube, banks - and
checks that the browser is still alive after the page has had time to load. A crash keeps its
logcat crash buffer, its newest tombstone and the backtrace symbolicated by Mozilla's server
(symbolicate.py). The report goes to stdout (the job log, which a cloud session can read), to a
Markdown file for the issue comment and to the job summary.

  scripts/ci/release_smoke.py --apk app-full-localRelease.apk --package io.github.mikespl187.vola.preview \\
      --report smoke.md
Exits 1 when any site crashed the browser.
"""

import argparse
import dataclasses
import os
import subprocess
import sys
import time
from pathlib import Path

sys.path.insert(0, str(Path(__file__).resolve().parent))
import symbolicate  # noqa: E402

# Sites people open every day, and the ones that crashed before (#123, H1: Google sign-in).
SITES = (
    ("Google: вход", "https://accounts.google.com/"),
    ("Google: аккаунт", "https://accounts.google.com/ServiceLogin?continue=https%3A%2F%2Fmyaccount.google.com%2F"),
    ("VK ID", "https://id.vk.ru/"),
    ("VK", "https://vk.com/"),
    ("Почта Mail", "https://account.mail.ru/login"),
    ("YouTube", "https://m.youtube.com/"),
    ("YouTube: видео", "https://www.youtube.com/watch?v=jNQXAC9IVRw"),
    ("Сбербанк Онлайн", "https://online.sberbank.ru/"),
    ("Т-Банк", "https://www.tbank.ru/login/"),
    ("ВТБ Онлайн", "https://online.vtb.ru/"),
    ("Госуслуги", "https://www.gosuslugi.ru/"),
    ("Яндекс", "https://ya.ru/"),
    ("Википедия", "https://ru.wikipedia.org/"),
)


@dataclasses.dataclass
class SiteResult:
    name: str
    url: str
    alive: bool
    crash_log: str = ""
    tombstone: str = ""
    symbolicated: list = dataclasses.field(default_factory=list)
    loaded: bool = True  # Gecko started a content process for the page

    @property
    def unopened(self):
        return not self.crashed and not self.loaded

    @property
    def java_crash(self):
        return "FATAL EXCEPTION" in self.crash_log

    @property
    def crashed(self):
        return not self.alive or bool(self.crash_log.strip())


def adb(*args, check=False, timeout=120):
    result = subprocess.run(["adb", *args], capture_output=True, text=True, errors="replace",
                            timeout=timeout, check=check)
    return result.stdout


def wait_for_boot(timeout=300):
    adb("wait-for-device", timeout=timeout)
    deadline = time.time() + timeout
    while time.time() < deadline:
        if adb("shell", "getprop", "sys.boot_completed").strip() == "1":
            adb("shell", "input", "keyevent", "82")
            return
        time.sleep(3)
    raise SystemExit("the emulator did not boot")


def newest_tombstone(since):
    """The text tombstone written after [since] (a set of names seen before), or ""."""
    names = adb("shell", "ls", "-t", "/data/tombstones").split()
    for name in names:
        if name not in since and not name.endswith(".pb"):
            return adb("shell", "cat", f"/data/tombstones/{name}")[:20000]
    return ""


def screenshot(path):
    shot = subprocess.run(["adb", "exec-out", "screencap", "-p"], capture_output=True, timeout=60).stdout
    if shot:
        Path(path).write_bytes(shot)


def check_site(package, name, url, settle, shot=None):
    adb("shell", "am", "force-stop", package)
    adb("logcat", "-b", "all", "-c")
    before = set(adb("shell", "ls", "/data/tombstones").split())
    adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", f"'{url}'", "-p", package)
    time.sleep(settle)
    alive = bool(adb("shell", "pidof", package).strip())
    # Gecko renders pages in "<package>:tabN"; without one the page never started loading.
    loaded = f"{package}:tab" in adb("shell", "ps", "-A", "-o", "NAME")
    if shot:
        screenshot(shot)
    crash_log = adb("logcat", "-d", "-b", "crash")
    tombstone = newest_tombstone(before) if not alive or crash_log.strip() else ""
    result = SiteResult(name, url, alive, crash_log, tombstone, loaded=loaded)
    if result.crashed:
        result.symbolicated = symbolicate.symbolicate(crash_log + "\n" + tombstone)
    return result


def report(results, version):
    crashed = [result for result in results if result.crashed]
    unopened = [result for result in results if result.unopened]
    verdict = "✅ без вылетов" if not crashed else f"❌ вылетов: {len(crashed)} из {len(results)}"
    if unopened:
        verdict += f" · ⚠️ не открылись: {len(unopened)}"
    lines = [f"## Ночной smoke-тест release · {version} · {verdict}", "", "| Сайт | Результат |", "|---|---|"]
    for result in results:
        if result.unopened:
            status = "⚠️ страница не открылась (нет процесса вкладки)"
        elif not result.crashed:
            status = "✅ жив"
        elif result.java_crash:
            status = "❌ вылет (Java)"
        else:
            status = "❌ вылет" if not result.alive else "⚠️ сбой в журнале, процесс жив"
        lines.append(f"| [{result.name}]({result.url}) | {status} |")
    for result in crashed:
        lines += ["", f"### {result.name}", f"`{result.url}`", ""]
        for number, block in enumerate(result.symbolicated, start=1):
            lines += [f"Стек {number} (расшифровка symbolication.services.mozilla.com):", "```", *block, "```"]
        tail = result.crash_log.strip().splitlines()[-40:]
        if tail:
            lines += ["<details><summary>Журнал сбоев (logcat -b crash)</summary>", "", "```", *tail, "```",
                      "</details>"]
    return "\n".join(lines) + "\n"


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--apk", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--report", required=True)
    parser.add_argument("--version", default=os.environ.get("GITHUB_SHA", "")[:7] or "local")
    parser.add_argument("--settle", type=int, default=35, help="seconds a page gets to load")
    parser.add_argument("--screenshots", help="directory for a screenshot of each site")
    args = parser.parse_args(argv)

    wait_for_boot()
    adb("root")  # tombstones are readable only as root; google_apis images allow it
    wait_for_boot()
    adb("install", "-r", "-g", args.apk, check=True, timeout=600)
    adb("shell", "pm", "grant", args.package, "android.permission.POST_NOTIFICATIONS")

    results = []
    shots = Path(args.screenshots) if args.screenshots else None
    if shots:
        shots.mkdir(parents=True, exist_ok=True)
    for number, (name, url) in enumerate(SITES, start=1):
        shot = shots / f"{number:02d}.png" if shots else None
        result = check_site(args.package, name, url, args.settle, shot)
        label = "CRASH" if result.crashed else "EMPTY" if result.unopened else "ok"
        print(f"{label:5} {name} {url}", flush=True)
        results.append(result)

    text = report(results, args.version)
    Path(args.report).write_text(text, encoding="utf-8")
    print(text, flush=True)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(text)
    return 1 if any(result.crashed for result in results) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
