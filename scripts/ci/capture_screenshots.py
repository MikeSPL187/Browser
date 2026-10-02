#!/usr/bin/env python3
"""Drives an installed Vola build on a running emulator and saves screenshots of key screens.

Usage: capture_screenshots.py <package> <output-dir>

Next to the output directory it keeps ui-dumps/: the UI hierarchy behind every screenshot, read by
a11y_audit.py, and the screen density.

Navigation uses visible text and accessibility descriptions (English and Russian), so the script
keeps working when layouts change. Every step is best effort: a failed step is logged and the
tour continues, so one broken screen never hides the rest.
"""

import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ElementTree
from pathlib import Path

PACKAGE = sys.argv[1]
OUT = Path(sys.argv[2])
OUT.mkdir(parents=True, exist_ok=True)
DUMPS = OUT.parent / "ui-dumps"
DUMPS.mkdir(parents=True, exist_ok=True)
LOG = []
_counter = 0


def adb(*args, check=True, capture=False, timeout=60):
    result = subprocess.run(
        ["adb", *args],
        check=check,
        stdout=subprocess.PIPE if capture else None,
        stderr=subprocess.PIPE if capture else None,
        timeout=timeout,
    )
    return result.stdout if capture else None


def log(message):
    print(message, flush=True)
    LOG.append(message)


# A slow emulator can raise "System UI isn't responding" over the app. While it is open,
# uiautomator sees only the dialog: taps miss and screenshots show the dialog, not Vola.
NOT_RESPONDING_MARKERS = ("isn't responding", "not responding", "не отвечает")
WAIT_LABELS = ("Wait", "Подождать", "Ждать")


def dump_ui():
    """The current UI hierarchy as raw XML, after closing a system "not responding" dialog."""
    raw = b""
    for _ in range(3):
        adb("shell", "uiautomator", "dump", "/sdcard/vola-ui.xml", check=False, capture=True)
        raw = adb("exec-out", "cat", "/sdcard/vola-ui.xml", check=False, capture=True) or b""
        if not dismiss_not_responding_dialog(raw):
            break
        time.sleep(2)
    return raw


def dismiss_not_responding_dialog(raw):
    """Taps "Wait" on a system "not responding" dialog in [raw]; True if there was one."""
    text = raw.decode("utf-8", "replace")
    if not any(marker in text for marker in NOT_RESPONDING_MARKERS):
        return False
    for node in parse_nodes(raw):
        if node["text"] in WAIT_LABELS or node["desc"] in WAIT_LABELS:
            x, y = node["center"]
            adb("shell", "input", "tap", str(x), str(y))
            log(f"dismissed a system 'not responding' dialog at {x},{y}")
            return True
    return False


def shot(name):
    global _counter
    _counter += 1
    target = OUT / f"{_counter:02d}-{name}.png"
    raw = dump_ui()
    with target.open("wb") as file:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=file, check=True, timeout=60)
    log(f"screenshot {target.name}")
    if raw.lstrip().startswith(b"<?xml"):
        (DUMPS / f"{target.stem}.xml").write_bytes(raw)


def nodes():
    return parse_nodes(dump_ui())


def parse_nodes(raw):
    try:
        root = ElementTree.fromstring(raw.decode("utf-8", "replace"))
    except ElementTree.ParseError:
        return []
    found = []
    for node in root.iter("node"):
        bounds = re.findall(r"\d+", node.get("bounds", ""))
        if len(bounds) != 4:
            continue
        left, top, right, bottom = map(int, bounds)
        found.append({
            "text": node.get("text", ""),
            "desc": node.get("content-desc", ""),
            "center": ((left + right) // 2, (top + bottom) // 2),
        })
    return found


def find(*labels, contains=False):
    for node in nodes():
        for value in (node["text"], node["desc"]):
            for label in labels:
                if (contains and label.lower() in value.lower()) or value == label:
                    return node
    return None


def tap(*labels, contains=False):
    node = find(*labels, contains=contains)
    if node is None:
        log(f"not found: {labels}")
        return False
    x, y = node["center"]
    adb("shell", "input", "tap", str(x), str(y))
    log(f"tap {labels[0]!r} at {x},{y}")
    return True


def save_ui(name):
    """Keeps the UI hierarchy next to the screenshots, so a missed element can be diagnosed."""
    (OUT / f"ui-{name}.xml").write_bytes(dump_ui())
    log(f"saved ui-{name}.xml")


def tap_scrolling(*labels, name, attempts=7):
    """Taps a label, scrolling the visible list up between attempts when it is off screen."""
    width, height = screen_size()
    for _ in range(attempts):
        if tap(*labels):
            return True
        adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.8)),
            str(width // 2), str(int(height * 0.4)), "300")
        time.sleep(1)
    save_ui(name)
    return False


def screen_size():
    output = (adb("shell", "wm", "size", capture=True) or b"").decode()
    match = re.search(r"(\d+)x(\d+)", output)
    return (int(match.group(1)), int(match.group(2))) if match else (1080, 2400)


def launch():
    adb("shell", "monkey", "-p", PACKAGE, "-c", "android.intent.category.LAUNCHER", "1",
        check=False, capture=True)


def open_url(url):
    adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", url, PACKAGE,
        check=False, capture=True)


def step(name, action):
    try:
        action()
    except Exception as error:  # noqa: BLE001 - keep the tour going
        log(f"step {name} failed: {error}")


def dismiss_first_run():
    for _ in range(6):
        if find("Skip", "Пропустить"):
            shot("onboarding")
            tap("Skip", "Пропустить")
        elif find("Explore Vola", "К браузеру"):
            shot("whats-new")
            tap("Explore Vola", "К браузеру")
        else:
            return
        time.sleep(3)


def tour(suffix):
    width, height = screen_size()
    step("new-tab", lambda: shot(f"new-tab-{suffix}"))

    def page():
        open_url("https://en.wikipedia.org/wiki/Zen")
        time.sleep(15)
        shot(f"page-{suffix}")
        # Scrolling down the page compacts the address bar. The first scroll event only sets the
        # baseline, so scroll twice.
        for _ in range(2):
            adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.7)),
                str(width // 2), str(int(height * 0.4)), "600")
            time.sleep(1)
        time.sleep(2)
        shot(f"page-scrolled-{suffix}")
    step("page", page)

    def find_in_page():
        if not find("More options", "Другие действия"):
            # The scrolled page left the compact capsule, which exposes no label: tap its spot.
            adb("shell", "input", "tap", str(width // 2), str(int(height * 0.94)))
            time.sleep(2)
        if not tap("More options", "Другие действия"):
            log("not found: menu for find in page")
            return
        time.sleep(2)
        if not tap_scrolling("Find on page", "Найти на странице", name=f"find-menu-{suffix}"):
            return
        time.sleep(2)
        adb("shell", "input", "text", "Zen")
        time.sleep(3)
        shot(f"find-{suffix}")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)
    step("find", find_in_page)

    def https_upgrade():
        # example.com serves HTTPS, so HTTPS-only mode must upgrade this link (lock in the bar).
        open_url("http://example.com/")
        time.sleep(12)
        shot(f"https-upgrade-{suffix}")
    step("https-upgrade", https_upgrade)

    def https_only_warning():
        # neverssl.com deliberately avoids HTTPS, so HTTPS-only mode ends on its warning page.
        open_url("http://neverssl.com/")
        time.sleep(10)
        shot(f"https-only-early-{suffix}")
        time.sleep(30)
        shot(f"https-only-{suffix}")
    step("https-only", https_only_warning)

    def overview():
        address = find("wikipedia.org", contains=True)
        x, y = address["center"] if address else (width // 2, height - 120)
        adb("shell", "input", "swipe", str(x), str(y), str(x), str(int(height * 0.35)), "350")
        time.sleep(3)
        shot(f"tab-overview-{suffix}")
        workspace = find("Personal", "Личное", contains=True)
        if workspace:
            wx, wy = workspace["center"]
            adb("shell", "input", "swipe", str(wx), str(wy), str(wx), str(wy), "900")
            time.sleep(2)
            shot(f"workspace-options-{suffix}")
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
        else:
            log("not found: workspace switcher entry")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("overview", overview)

    def essentials():
        # A new tab from the tab overview shows the workspace's Essentials (Q5a): the empty
        # state on a fresh install, the add sheet, the grid and its edit mode.
        adb("shell", "input", "swipe", str(width // 2), str(height - 120),
            str(width // 2), str(int(height * 0.35)), "350")
        time.sleep(3)
        if not tap("New tab", "Новая вкладка"):
            save_ui(f"essentials-overview-{suffix}")
            adb("shell", "input", "keyevent", "BACK")
            return
        time.sleep(3)
        # A new tab opens the address editor with the keyboard; close it to see the page.
        if find("Switch to tab", "Перейти во вкладку", "Search or enter a URL", "Поиск или адрес сайта",
                contains=True):
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
        shot(f"essentials-start-{suffix}")
        if tap("Add a site", "Добавить сайт"):
            time.sleep(2)
            shot(f"essentials-add-{suffix}")
            for _ in range(2):
                node = next((n for n in nodes() if n["desc"].startswith(("Add ", "Добавить «"))), None)
                if node is None:
                    break
                x, y = node["center"]
                adb("shell", "input", "tap", str(x), str(y))
                time.sleep(2)
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
        shot(f"essentials-{suffix}")
        if tap("Edit", "Изменить"):
            time.sleep(2)
            shot(f"essentials-edit-{suffix}")
            tap("Done", "Готово")
            time.sleep(1)
    step("essentials", essentials)

    def menu_and_settings():
        if tap("More options", "Другие действия"):
            time.sleep(2)
            shot(f"menu-{suffix}")
            if tap_scrolling("Settings", "Настройки", name=f"menu-{suffix}"):
                time.sleep(3)
                shot(f"settings-{suffix}")
                if tap_scrolling("Appearance", "Внешний вид", name=f"settings-{suffix}"):
                    time.sleep(2)
                    shot(f"appearance-{suffix}")
                    adb("shell", "input", "keyevent", "BACK")
                    time.sleep(1)
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
    step("settings", menu_and_settings)


def accessibility_pass():
    """Key screens with the largest system font (200 %). The workflow already turns animations off
    for the whole tour, so these shots also show the reduced-motion state."""
    adb("shell", "settings", "put", "system", "font_scale", "2.0", check=False)
    adb("shell", "am", "force-stop", PACKAGE, check=False)
    launch()
    time.sleep(12)
    step("new-tab-a11y", lambda: shot("new-tab-a11y"))

    def page_and_menu():
        open_url("https://en.wikipedia.org/wiki/Zen")
        time.sleep(15)
        shot("page-a11y")
        if tap("More options", "Другие действия"):
            time.sleep(2)
            shot("menu-a11y")
            if tap_scrolling("Settings", "Настройки", name="a11y-menu"):
                time.sleep(3)
                shot("settings-a11y")
                adb("shell", "input", "keyevent", "BACK")
                time.sleep(1)
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(1)
    step("page-a11y", page_and_menu)

    adb("shell", "settings", "put", "system", "font_scale", "1.0", check=False)


def measure_cold_start(runs=5):
    """Cold start time (am start -W, TotalTime) for the summary: a trend, not a gate."""
    resolved = (adb("shell", "cmd", "package", "resolve-activity", "--brief", "-c",
                    "android.intent.category.LAUNCHER", PACKAGE, check=False, capture=True) or b"")
    component = resolved.decode().strip().splitlines()[-1] if resolved.strip() else ""
    if "/" not in component:
        log("cold start: launcher activity not found")
        return
    times = []
    for _ in range(runs):
        adb("shell", "am", "force-stop", PACKAGE, check=False)
        time.sleep(2)
        output = (adb("shell", "am", "start", "-W", "-n", component, check=False, capture=True,
                      timeout=90) or b"").decode()
        match = re.search(r"TotalTime:\s*(\d+)", output)
        if match:
            times.append(int(match.group(1)))
        time.sleep(3)
    if times:
        summary = (f"Cold start (am start -W, {len(times)} runs): average {sum(times) // len(times)} ms, "
                   f"min {min(times)} ms, max {max(times)} ms")
    else:
        summary = "Cold start: no TotalTime reported"
    (OUT / "startup.txt").write_text(summary + "\n")
    log(summary)


def air_layout(suffix):
    """Switches Appearance → Browser layout to Air and shoots the edge-to-edge page."""
    if not tap("More options", "Другие действия"):
        log("not found: menu for the Air layout")
        return
    time.sleep(2)
    if not tap_scrolling("Settings", "Настройки", name=f"air-menu-{suffix}"):
        return
    time.sleep(3)
    if not tap_scrolling("Appearance", "Внешний вид", name=f"air-settings-{suffix}"):
        return
    time.sleep(2)
    if not tap("Browser layout", "Оформление"):
        log("not found: browser layout choice")
        return
    time.sleep(1)
    if not tap("Air", "Воздух"):
        log("not found: Air layout")
        return
    time.sleep(1)
    for _ in range(3):
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)
    open_url("https://en.wikipedia.org/wiki/Zen")
    time.sleep(12)
    shot(f"page-air-{suffix}")


def main():
    density = (adb("shell", "wm", "density", capture=True, check=False) or b"").decode()
    match = re.search(r"(\d+)\s*$", density.strip())
    (DUMPS / "density.txt").write_text((match.group(1) if match else "420") + "\n")
    adb("shell", "cmd", "uimode", "night", "no", check=False)
    launch()
    time.sleep(15)
    shot("launch")
    dismiss_first_run()
    tour("light")

    adb("shell", "cmd", "uimode", "night", "yes", check=False)
    time.sleep(5)
    step("dark-new-tab", lambda: shot("current-dark"))
    tour("dark")

    adb("shell", "cmd", "uimode", "night", "no", check=False)
    step("accessibility", accessibility_pass)
    step("cold-start", measure_cold_start)
    adb("shell", "cmd", "locale", "set-app-locales", PACKAGE, "--locales", "ru-RU", check=False)
    adb("shell", "am", "force-stop", PACKAGE, check=False)
    launch()
    time.sleep(12)
    dismiss_first_run()
    tour("ru")
    step("air", lambda: air_layout("ru"))

    crashes = (adb("logcat", "-d", "-b", "crash", check=False, capture=True) or b"").decode(
        "utf-8", "replace"
    )
    (OUT / "crash-log.txt").write_text(crashes or "No crashes recorded.\n")
    gecko = (adb("logcat", "-d", check=False, capture=True) or b"").decode("utf-8", "replace")
    https_lines = [
        line for line in gecko.splitlines()
        if re.search(r"https.?only|HTTPS-Only|onLoadError|LoadURIDelegate|neverssl", line, re.I)
    ]
    (OUT / "https-only-log.txt").write_text("\n".join(https_lines[-400:]) + "\n")
    (OUT / "tour-log.txt").write_text("\n".join(LOG) + "\n")


if __name__ == "__main__":
    main()
