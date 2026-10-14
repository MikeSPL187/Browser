#!/usr/bin/env python3
"""Nightly smoke test of the release build on an emulator (.github/workflows/nightly-smoke.yml).

Opens popular sites one by one in a fresh process - Google and VK sign-in, YouTube, banks - and
checks that the browser is still alive after the page has had time to load. A crash keeps its
logcat crash buffer, its newest tombstone and the backtrace symbolicated by Mozilla's server
(symbolicate.py). The report goes to stdout (the job log, which a cloud session can read), to a
Markdown file for the issue comment and to the job summary.

  scripts/ci/release_smoke.py --apk app-full-localRelease.apk --package io.github.mikespl187.vola.preview \\
      --report smoke.md
Exits 1 when any site crashed the browser. When the emulator itself hangs (adb gets no answer or
says the device is offline, or Android's own system_server dies and takes every app with it), the
site is marked as not checked, the emulator gets a few minutes to come back and the run goes on; a
hang is never counted as a crash of the browser.
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
    ("Сбербанк Онлайн", "https://online.sberbank.ru/"),
    ("Т-Банк", "https://www.tbank.ru/login/"),
    ("ВТБ Онлайн", "https://online.vtb.ru/"),
    ("Госуслуги", "https://www.gosuslugi.ru/"),
    ("Яндекс", "https://ya.ru/"),
    ("Википедия", "https://ru.wikipedia.org/"),
    # Last: video on the emulator's software GPU is the heaviest page and has stalled the emulator.
    ("YouTube: видео", "https://www.youtube.com/watch?v=jNQXAC9IVRw"),
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
    # When the process is gone without a crash log: why Android says it ended, what `am start` said
    # and the system log around it. Without these a quiet death (killed for memory, the app ending
    # itself) reads exactly like a crash with no trace.
    exit_info: str = ""
    start_output: str = ""
    system_log: str = ""
    # The emulator hung on this site, so nothing about the browser is known; [stall] says how.
    stalled: bool = False
    stall: str = ""

    @property
    def quiet_death(self):
        return self.crashed and not self.alive and not self.crash_log.strip()

    @property
    def unopened(self):
        return not self.stalled and not self.crashed and not self.loaded

    @property
    def java_crash(self):
        return "FATAL EXCEPTION" in self.crash_log

    @property
    def crashed(self):
        return not self.stalled and (not self.alive or bool(self.crash_log.strip()))


class EmulatorStuck(Exception):
    """adb got no answer or Android itself restarted: the emulator, not the browser, stopped working."""


# What adb says on stderr when it lost the emulator, rather than when the command failed on it.
LOST_DEVICE_MARKERS = ("device offline", "no devices/emulators found", "device not found",
                       "device still connecting", "error: closed", "protocol fault")

# Hangs a run gets over before it stops: a hang that keeps coming back is no longer one bad page.
MAX_STALLS = 2


def adb(*args, check=False, timeout=120):
    # A hang or a lost device is never read as empty output: an empty pidof would look like a crash.
    # Once the emulator hung, adb fails at once with "device offline", and every later site read
    # as a crash with no trace (nightly runs 9.10-13.10, #134).
    try:
        result = subprocess.run(["adb", *args], capture_output=True, text=True, errors="replace",
                                timeout=timeout, check=check)
    except subprocess.TimeoutExpired:
        raise EmulatorStuck(f"adb {' '.join(args)}: no answer in {timeout} s") from None
    error = (result.stderr or "").strip()
    if result.returncode != 0 and any(marker in error.lower() for marker in LOST_DEVICE_MARKERS):
        raise EmulatorStuck(f"adb {' '.join(args)}: {error.splitlines()[-1]}")
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


def recover(timeout=240):
    """Whether the emulator answers again after a hang; it gets [timeout] seconds."""
    try:
        adb("reconnect", "offline", timeout=30)
    except EmulatorStuck:
        pass
    try:
        wait_for_boot(timeout)
        return adb("shell", "echo", "ok", timeout=30).strip() == "ok"
    except (EmulatorStuck, SystemExit):
        return False


def newest_tombstone(since):
    """The text tombstone written after [since] (a set of names seen before), or ""."""
    names = adb("shell", "ls", "-t", "/data/tombstones").split()
    for name in names:
        if name not in since and not name.endswith(".pb"):
            return adb("shell", "cat", f"/data/tombstones/{name}")[:20000]
    return ""


def stuck_diagnostics():
    """What the emulator was doing when adb stopped answering, for the job log; each probe may hang too."""
    # The runner's own memory first: a runner swapping to disk stalls the emulator's disk.
    print("::group::stuck emulator: runner memory", flush=True)
    try:
        print(subprocess.run(["free", "-m"], capture_output=True, text=True, timeout=10).stdout, flush=True)
    except (OSError, subprocess.TimeoutExpired) as error:
        print(error, flush=True)
    print("::endgroup::", flush=True)
    for title, command in (
        ("memory", ["shell", "cat", "/proc/meminfo"]),
        ("load", ["shell", "top", "-b", "-n", "1", "-m", "15"]),
        ("logcat (last 150 lines)", ["logcat", "-d", "-t", "150"]),
    ):
        print(f"::group::stuck emulator: {title}", flush=True)
        try:
            print(adb(*command, timeout=30)[:20000], flush=True)
        except EmulatorStuck as error:
            print(error, flush=True)
        print("::endgroup::", flush=True)


# The log lines that tell why a process ended or never started.
SYSTEM_LOG_MARKERS = ("ActivityManager", "ActivityTaskManager", "lowmemorykiller", "lmkd", "libprocessgroup",
                      "GeckoRuntime", "GeckoThread", "Gecko", "AndroidRuntime", "DEBUG", "Process", "Vola")


def exit_reasons(dump, limit=12):
    """The ApplicationExitInfo records of `dumpsys activity exit-info`, newest first, a few lines each."""
    records, current = [], []
    for line in dump.splitlines():
        stripped = line.strip()
        if stripped.startswith("ApplicationExitInfo #"):
            if current:
                records.append(current)
            current = [stripped]
        elif current and stripped and len(current) < 6:
            current.append(stripped)
    if current:
        records.append(current)
    return "\n".join("\n".join(record) for record in records[:limit])


def system_log_tail(log, package, limit=120):
    """The lines of the main and system logs that speak of the package or of process deaths."""
    lines = [line for line in log.splitlines()
             if package in line or any(marker in line for marker in SYSTEM_LOG_MARKERS)]
    return "\n".join(lines[-limit:])


def system_server_pid():
    return adb("shell", "pidof", "system_server").strip()


def system_restart(before, after):
    """How Android's system_server changed while a site was open, or "" when it stayed.

    When it dies (ANR, then killed) every app goes with it and leaves no crash log of its own: the
    night of 14.10 counted six such deaths as crashes of the browser (#134).
    """
    if not before or after == before:
        return ""
    return f"system_server перезапустился (pid {before} → {after or 'нет'})"


def check_site(package, name, url, settle):
    system_before = system_server_pid()
    adb("shell", "am", "force-stop", package)
    adb("logcat", "-b", "all", "-c")
    before = set(adb("shell", "ls", "/data/tombstones").split())
    start_output = adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", f"'{url}'", "-p", package)
    time.sleep(settle)
    restart = system_restart(system_before, system_server_pid())
    if restart:
        raise EmulatorStuck(restart)
    alive = bool(adb("shell", "pidof", package).strip())
    # Gecko renders pages in "<package>:tabN"; without one the page never started loading.
    # No screencap here: on the emulator's software GPU it hangs the system while a video plays.
    loaded = f"{package}:tab" in adb("shell", "ps", "-A", "-o", "NAME")
    crash_log = adb("logcat", "-d", "-b", "crash")
    tombstone = newest_tombstone(before) if not alive or crash_log.strip() else ""
    result = SiteResult(name, url, alive, crash_log, tombstone, loaded=loaded, start_output=start_output.strip())
    if result.crashed:
        result.symbolicated = symbolicate.symbolicate(crash_log + "\n" + tombstone)
    if result.quiet_death:
        result.exit_info = exit_reasons(adb("shell", "dumpsys", "activity", "exit-info", package))
        result.system_log = system_log_tail(adb("logcat", "-d", "-b", "main,system,events", "-t", "2000"), package)
    return result


def report(results, version, aborted=""):
    crashed = [result for result in results if result.crashed]
    unopened = [result for result in results if result.unopened]
    verdict = "✅ без вылетов" if not crashed else f"❌ вылетов: {len(crashed)} из {len(results)}"
    if unopened:
        verdict += f" · ⚠️ не открылись: {len(unopened)}"
    stalled = [result for result in results if result.stalled]
    if stalled:
        verdict += f" · ⏸ эмулятор зависал: {len(stalled)}"
    if aborted:
        verdict += f" · ⚠️ прогон прерван: проверено {len(results)} из {len(SITES)}"
    lines = [f"## Ночной smoke-тест release · {version} · {verdict}", ""]
    if aborted:
        lines += [f"Прерван: `{aborted}`", ""]
    lines += ["| Сайт | Результат |", "|---|---|"]
    for result in results:
        if result.stalled:
            status = "⏸ эмулятор завис, сайт не проверен"
        elif result.unopened:
            status = "⚠️ страница не открылась (нет процесса вкладки)"
        elif not result.crashed:
            status = "✅ жив"
        elif result.java_crash:
            status = "❌ вылет (Java)"
        else:
            status = "❌ вылет" if not result.alive else "⚠️ сбой в журнале, процесс жив"
        lines.append(f"| [{result.name}]({result.url}) | {status} |")
    for result in stalled:
        lines += ["", f"⏸ {result.name}: `{result.stall}`"]
    for result in crashed:
        lines += ["", f"### {result.name}", f"`{result.url}`", ""]
        for number, block in enumerate(result.symbolicated, start=1):
            lines += [f"Стек {number} (расшифровка symbolication.services.mozilla.com):", "```", *block, "```"]
        tail = result.crash_log.strip().splitlines()[-40:]
        if tail:
            lines += ["<details><summary>Журнал сбоев (logcat -b crash)</summary>", "", "```", *tail, "```",
                      "</details>"]
        if result.quiet_death:
            lines += ["Процесс завершился без журнала сбоев.", ""]
            for title, text in (
                ("Причина выхода (dumpsys activity exit-info)", result.exit_info),
                ("am start", result.start_output),
                ("Журнал системы (процессы и Gecko)", result.system_log),
            ):
                if text:
                    lines += [f"<details><summary>{title}</summary>", "", "```", *text.splitlines(), "```",
                              "</details>"]
    return "\n".join(lines) + "\n"


def aborted_at(reason):
    print(f"ABORT {reason}", flush=True)
    return reason


def run_sites(package, settle, check=None, recover_emulator=None):
    """The result of every site, and why the run stopped early ("" when it did not)."""
    check = check or check_site
    recover_emulator = recover_emulator or recover
    results = []
    stalls = 0
    for name, url in SITES:
        try:
            result = check(package, name, url, settle)
        except EmulatorStuck as error:  # the sites checked so far still make the report
            print(f"STALL {name}: {error}", flush=True)
            stuck_diagnostics()
            stalls += 1
            if stalls > MAX_STALLS or not recover_emulator():
                return results, aborted_at(f"{name}: эмулятор перестал отвечать ({error})")
            results.append(SiteResult(name, url, alive=True, stalled=True, stall=str(error)))
            continue
        except Exception as error:
            return results, aborted_at(f"{name}: {type(error).__name__}: {error}")
        label = "CRASH" if result.crashed else "EMPTY" if result.unopened else "ok"
        print(f"{label:5} {name} {url}", flush=True)
        results.append(result)
    return results, ""


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    parser.add_argument("--apk", required=True)
    parser.add_argument("--package", required=True)
    parser.add_argument("--report", required=True)
    parser.add_argument("--version", default=os.environ.get("GITHUB_SHA", "")[:7] or "local")
    parser.add_argument("--settle", type=int, default=35, help="seconds a page gets to load")
    args = parser.parse_args(argv)

    wait_for_boot()
    adb("root")  # tombstones are readable only as root; google_apis images allow it
    wait_for_boot()
    adb("install", "-r", "-g", args.apk, check=True, timeout=600)
    adb("shell", "pm", "grant", args.package, "android.permission.POST_NOTIFICATIONS")

    results, aborted = run_sites(args.package, args.settle)

    text = report(results, args.version, aborted)
    Path(args.report).write_text(text, encoding="utf-8")
    print(text, flush=True)
    summary = os.environ.get("GITHUB_STEP_SUMMARY")
    if summary:
        with open(summary, "a", encoding="utf-8") as handle:
            handle.write(text)
    return 1 if aborted or any(result.crashed for result in results) else 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
