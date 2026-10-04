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
ADD_WORKSPACE_LABELS = ("Add workspace", "Добавить пространство")
# The workspace the tour makes for the swipe: no name, so the default one.
SECOND_WORKSPACE_LABELS = ("Workspace", "Пространство")
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


def shot(name, audit=True):
    """A screenshot and, for the accessibility audit, the UI behind it. A frame caught in the
    middle of a gesture is not a resting screen, so it stays out of the audit."""
    global _counter
    _counter += 1
    target = OUT / f"{_counter:02d}-{name}.png"
    raw = dump_ui()
    with target.open("wb") as file:
        subprocess.run(["adb", "exec-out", "screencap", "-p"], stdout=file, check=True, timeout=60)
    log(f"screenshot {target.name}")
    if audit and raw.lstrip().startswith(b"<?xml"):
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


def scroll_to(*labels, name, attempts=5):
    """Scrolls the visible list up until a label (or a row containing it) is on screen."""
    width, height = screen_size()
    for _ in range(attempts):
        if find(*labels, contains=True):
            return True
        adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.7)),
            str(width // 2), str(int(height * 0.45)), "300")
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
        # The counter as the find bar shows it: "1/541", "1/…" or none while counting ("0/0" with
        # highlighted matches was a GeckoView race, see FindInPageRules.withResult).
        counter = next((node["text"] for node in nodes()
                        if re.fullmatch(r"\d+/(\d+|…)", node["text"])), None)
        log(f"find counter {suffix}: {counter or 'counting'}")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)
    step("find", find_in_page)

    def site_info():
        """Site information (board W-SiteInfo) from the badge in the address bar, then X-Ray."""
        labels = ("Open site information", "Открыть сведения о сайте")
        if not find(*labels, contains=True):
            # The scrolled page left the compact capsule, which exposes no label: tap its spot.
            adb("shell", "input", "tap", str(width // 2), str(int(height * 0.94)))
            time.sleep(2)
        if not tap(*labels, contains=True):
            log("not found: site information badge")
            return
        time.sleep(3)
        shot(f"site-info-{suffix}")
        save_ui(f"site-info-{suffix}")
        # «Certificate» under the connection row (Q10b): who issued it, dates, SHA-256.
        if tap("Certificate", "Сертификат", contains=True):
            time.sleep(2)
            shot(f"site-certificate-{suffix}")
            if tap("Back", "Назад"):
                time.sleep(1)
        if tap("Privacy X-Ray", "Рентген приватности"):
            time.sleep(2)
            shot(f"site-info-xray-{suffix}")
            # The bar's Back returns to the overview; a missed tap must not leave the page.
            if tap("Back", "Назад"):
                time.sleep(1)
        # «Site data» at the end of the sheet (Q10b): «Delete» closes the sheet and waits behind
        # «Undo»; the tour takes the deletion back.
        if scroll_to("Site data", "Данные сайта", name=f"site-info-data-{suffix}"):
            shot(f"site-info-data-{suffix}")
            if tap("Delete data of", "Удалить данные", contains=True):
                time.sleep(1)
                shot(f"site-data-undo-{suffix}", audit=False)
                tap("Undo", "Отменить")
                time.sleep(1)
                return
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("site-info", site_info)

    def address_editor():
        # The address editor over the Zen page: library, search and open-tab suggestions (Q4).
        address = find("wikipedia.org", contains=True)
        if address is None:
            # The scrolled page left the compact capsule: tap it to bring the full bar back.
            adb("shell", "input", "tap", str(width // 2), str(int(height * 0.94)))
            time.sleep(2)
            address = find("wikipedia.org", contains=True)
        if address is None:
            log("not found: address field")
            return
        x, y = address["center"]
        adb("shell", "input", "tap", str(x), str(y))
        time.sleep(2)
        adb("shell", "input", "text", "zen")
        time.sleep(4)
        shot(f"address-{suffix}")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)
    step("address", address_editor)

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

    def workspace_swipe():
        """A swipe held halfway in the overview (board W-WorkspaceSwipe): the tabs slide aside and
        the next workspace's aura shows through. The light tour makes the second workspace."""
        if not find(*SECOND_WORKSPACE_LABELS):
            if not tap(*ADD_WORKSPACE_LABELS):
                return
            time.sleep(2)
            tap("Work", "Работа")
            # Its own color, so the swipe shows its aura coming through.
            tap("Coral", "Коралловый")
            time.sleep(1)
            if not tap("Create workspace", "Создать пространство"):
                adb("shell", "input", "keyevent", "BACK")
                return
            time.sleep(3)
        # The header names the active workspace; from the first one the swipe goes left.
        on_first = any(n["text"] in ("Personal", "Личное") for n in nodes())
        y = int(height * 0.075)
        start, end = (int(width * 0.8), int(width * 0.35)) if on_first else \
            (int(width * 0.2), int(width * 0.65))
        adb("shell", "input", "motionevent", "DOWN", str(start), str(y))
        for step_index in range(1, 9):
            x = start + (end - start) * step_index // 8
            adb("shell", "input", "motionevent", "MOVE", str(x), str(y))
            time.sleep(0.05)
        time.sleep(1)
        shot(f"workspace-swipe-{suffix}", audit=False)
        adb("shell", "input", "motionevent", "MOVE", str(start), str(y))
        adb("shell", "input", "motionevent", "UP", str(start), str(y))
        time.sleep(2)
        # Back to the first workspace, where the rest of the tour runs.
        if not any(n["text"] in ("Personal", "Личное") for n in nodes()):
            tap("Personal", "Личное")
            time.sleep(2)

    def overview():
        address = find("wikipedia.org", contains=True)
        x, y = address["center"] if address else (width // 2, height - 120)
        adb("shell", "input", "swipe", str(x), str(y), str(x), str(int(height * 0.35)), "350")
        time.sleep(3)
        shot(f"tab-overview-{suffix}")
        save_ui(f"tab-overview-{suffix}")
        # The workspace gem in the dock carries the name as its label; the header shows it as
        # text, which a long press would miss.
        workspace = next((n for n in nodes() if n["desc"] in ("Personal", "Личное")), None)
        if workspace:
            wx, wy = workspace["center"]
            adb("shell", "input", "swipe", str(wx), str(wy), str(wx), str(wy), "900")
            time.sleep(2)
            shot(f"workspace-options-{suffix}")
            save_ui(f"workspace-options-{suffix}")
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
        else:
            log("not found: workspace switcher entry")
        # «+» in the workspace dock opens «New workspace» (board W-WorkspaceSheet); Back leaves it
        # without creating anything.
        if tap(*ADD_WORKSPACE_LABELS):
            time.sleep(2)
            shot(f"workspace-new-{suffix}")
            # Two rows of icons until «All icons» opens the rest.
            if tap("All icons", "Все значки"):
                time.sleep(2)
                shot(f"workspace-new-icons-{suffix}")
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
        else:
            log("not found: add workspace")
        step("workspace-swipe", workspace_swipe)
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
        # A new tab opens the address editor with the keyboard. The first Back hides the keyboard,
        # the second closes the editor; stop as soon as the editor is gone.
        for _ in range(2):
            if not find("Switch to tab", "Перейти во вкладку", "Search or enter a URL",
                        "Поиск или адрес сайта", contains=True):
                break
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

    def overview_essentials():
        # Back in the tab overview the workspace's Essentials sit above the dock (Q7a).
        adb("shell", "input", "swipe", str(width // 2), str(height - 120),
            str(width // 2), str(int(height * 0.35)), "350")
        time.sleep(3)
        shot(f"tab-overview-essentials-{suffix}")
        save_ui(f"tab-overview-essentials-{suffix}")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("overview-essentials", overview_essentials)

    def tab_actions_and_search():
        # The tab actions sheet (Q7b, board W-TabActions) with «More» open, then tab search.
        adb("shell", "input", "swipe", str(width // 2), str(height - 120),
            str(width // 2), str(int(height * 0.35)), "350")
        time.sleep(3)
        if tap("Tab actions", "Действия со вкладкой"):
            time.sleep(2)
            shot(f"tab-actions-{suffix}")
            if tap("More", "Ещё"):
                time.sleep(2)
                shot(f"tab-actions-more-{suffix}")
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
        if tap("Search tabs", "Найти вкладку"):
            time.sleep(2)
            adb("shell", "input", "text", "wiki")
            time.sleep(2)
            shot(f"tab-search-{suffix}")
            # Hide the keyboard, close the search with its button, then leave the overview.
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(1)
            tap("Close search", "Закрыть поиск")
            time.sleep(1)
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("tab-actions", tab_actions_and_search)

    def protection_report():
        # The protection card below Essentials opens the weekly report (Q5b, П7).
        labels = ("this week", "за неделю", "Tracker protection is on", "Защита от трекеров")
        for _ in range(3):
            if find(*labels, contains=True):
                break
            adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.7)),
                str(width // 2), str(int(height * 0.4)), "300")
            time.sleep(1)
        shot(f"protection-card-{suffix}")
        if not tap(*labels, contains=True):
            save_ui(f"protection-card-{suffix}")
            return
        time.sleep(2)
        shot(f"protection-report-{suffix}")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("protection-report", protection_report)

    def private_tab():
        # The mask button of a blank tab turns it private (Q6): the purple page, then «Close all».
        # A private tab shows an ongoing notification; grant it up front so Android 13+ does not
        # cover the page with its permission dialog.
        adb("shell", "pm", "grant", PACKAGE, "android.permission.POST_NOTIFICATIONS", check=False)
        if not tap("Search or enter an address", "Поиск или адрес"):
            save_ui(f"private-start-{suffix}")
            return
        time.sleep(2)
        if not tap("Make blank tab incognito", "Сделать пустую вкладку приватной", contains=True):
            save_ui(f"private-toggle-{suffix}")
            adb("shell", "input", "keyevent", "BACK")
            return
        time.sleep(3)
        if tap("Don’t allow", "Don't allow", "Не разрешать", contains=True):
            time.sleep(2)
        for _ in range(2):
            if not find("Switch to tab", "Перейти во вкладку", "Search or enter a URL",
                        "Поиск или адрес сайта", contains=True):
                break
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
        shot(f"private-new-tab-{suffix}")
        # «Lock on exit» (П9) under the facts; without a biometric on the emulator it is off and
        # says why.
        if scroll_to("Lock on exit", "Замок при выходе", name=f"private-lock-{suffix}"):
            shot(f"private-lock-row-{suffix}")
        if tap("Close 1 private", "Close all", "Закрыть 1 приватную", "Закрыть все", contains=True):
            time.sleep(3)
            shot(f"private-closed-{suffix}")
    step("private-tab", private_tab)

    def private_tabs_locked():
        # Real private tabs cannot be locked without a biometric, so a debug-only activity shows
        # the «Private tabs locked» screen itself.
        adb("shell", "am", "start", "-n",
            f"{PACKAGE}/dev.sk2andy.materialbrowser.ui.PrivateTabsLockPreviewActivity",
            check=False, capture=True)
        time.sleep(3)
        shot(f"private-locked-{suffix}")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("private-locked", private_tabs_locked)

    def permission_prompt():
        # The permission request sheet (Q10c, board W-Permission). The tour has no page that asks
        # for a permission, so a debug-only activity shows the sheet itself.
        adb("shell", "am", "start", "-n",
            f"{PACKAGE}/dev.sk2andy.materialbrowser.ui.PermissionPromptPreviewActivity",
            check=False, capture=True)
        time.sleep(3)
        shot(f"permission-prompt-{suffix}")
        if not tap("Don’t allow", "Don't allow", "Запретить"):
            adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("permission-prompt", permission_prompt)

    def tab_archive_setting():
        """A lifetime in days brings up «Archive instead of closing»; the tour then sets it back."""
        lifetime = ("Automatically close tabs", "Автоматически закрывать вкладки")
        if not tap_scrolling(*lifetime, name=f"tabs-settings-{suffix}"):
            return
        time.sleep(1)
        if not tap("After 7 days", "Через 7 дней"):
            save_ui(f"tabs-lifetime-{suffix}")
            adb("shell", "input", "keyevent", "BACK")
            return
        time.sleep(2)
        width, height = screen_size()
        adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.7)),
            str(width // 2), str(int(height * 0.4)), "300")
        time.sleep(1)
        shot(f"tabs-archive-{suffix}")
        save_ui(f"tabs-archive-{suffix}")
        if tap(*lifetime):
            time.sleep(1)
            tap("Never", "Никогда")
            time.sleep(1)

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
                if tap_scrolling("Tabs & gestures", "Вкладки и жесты", name=f"settings-{suffix}"):
                    time.sleep(2)
                    tab_archive_setting()
                    adb("shell", "input", "keyevent", "BACK")
                    time.sleep(1)
                if tap_scrolling("Protection & data", "Защита и данные",
                                 name=f"settings-{suffix}"):
                    time.sleep(2)
                    if scroll_to("Lock private tabs on exit", "Запирать приватные вкладки",
                                 name=f"protection-private-lock-{suffix}", attempts=8):
                        shot(f"protection-private-lock-{suffix}")
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
