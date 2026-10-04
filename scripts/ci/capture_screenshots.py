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


# Sites without working HTTPS, most dependable first (see https_only_warning in the tour).
# httpforever.com reached the warning (ERROR_HTTPS_ONLY) in every pass of #61; info.cern.ch serves
# HTTPS. The rest stay as fallbacks in case it ever gains HTTPS.
HTTP_ONLY_CANDIDATES = (
    "http://httpforever.com/",
    "http://captive.apple.com/",
    "http://http.badssl.com/",
)


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


# A step that misses a tap can leave a surface open over the page. The next step would then
# shoot that surface instead of its own screen, and the audit would count its controls against
# the wrong frame, so every step closes what is left over.
LEFTOVER_SURFACES = (
    ("Close address input", "Закрыть ввод адреса"),
    ("Close Split View", "Выйти из Split View"),
)


def close_leftovers(name):
    """One look at the screen per step; a second only after something had to be closed."""
    current = nodes()
    for labels in LEFTOVER_SURFACES:
        node = next((node for node in current
                     if node["text"] in labels or node["desc"] in labels), None)
        if node is None:
            continue
        x, y = node["center"]
        adb("shell", "input", "tap", str(x), str(y))
        log(f"step {name}: closed a leftover {labels[0]!r}")
        time.sleep(2)
        current = nodes()


def settle(attempts=6):
    """Waits until the screen stops moving: two UI dumps in a row are the same. A frame caught
    mid-animation shows controls half covered, which the audit reads as too small."""
    previous = None
    for _ in range(attempts):
        raw = dump_ui()
        if raw and raw == previous:
            return True
        previous = raw
        time.sleep(1)
    log("screen did not settle")
    return False


def step(name, action):
    try:
        action()
    except Exception as error:  # noqa: BLE001 - keep the tour going
        log(f"step {name} failed: {error}")
    try:
        close_leftovers(name)
    except Exception as error:  # noqa: BLE001 - keep the tour going
        log(f"step {name}: leftovers not checked: {error}")


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

    def compact_mode():
        """Compact Mode (Q11, board W-Compact): on from the menu, the page fills the screen and
        the bar waits as a handle; a tap on the handle brings it back; off again from the menu."""
        open_url("https://en.wikipedia.org/wiki/Zen")
        time.sleep(10)
        if not tap("More options", "Другие действия"):
            return
        time.sleep(2)
        if not tap_scrolling("Compact mode", "Компактный режим", name=f"compact-menu-{suffix}"):
            adb("shell", "input", "keyevent", "BACK")
            return
        # The bar folds into the handle once the menu has closed; give it time on a slow emulator.
        time.sleep(5)
        shot(f"compact-mode-{suffix}")
        time.sleep(5)
        if tap("Show the bar", "Показать панель"):
            time.sleep(2)
            shot(f"compact-mode-bar-{suffix}")
        # Compact Mode is saved: it must be off again before the next step and pass.
        if not find("More options", "Другие действия"):
            if not tap("Show the bar", "Показать панель"):
                adb("shell", "input", "tap", str(width // 2), str(int(height * 0.955)))
            time.sleep(2)
        if tap("More options", "Другие действия"):
            time.sleep(2)
            if not tap_scrolling("Compact mode", "Компактный режим", name=f"compact-off-{suffix}"):
                adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
        else:
            log("compact mode: could not reach the menu to switch it off")
        time.sleep(1)
        if not find("More options", "Другие действия"):
            # Still compact: the rest of the tour would run without the bar.
            save_ui(f"compact-off-check-{suffix}")
            log("compact mode: still on after switching it off")
    step("compact-mode", compact_mode)

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
        address = None
        for _ in range(3):
            # «Undo» on the site data reloads the page; the bar shows the address once it is back.
            address = find("wikipedia.org", contains=True)
            if address is not None:
                break
            time.sleep(2)
        if address is None:
            # The scrolled page left the compact capsule: tap it to bring the full bar back.
            adb("shell", "input", "tap", str(width // 2), str(int(height * 0.94)))
            time.sleep(2)
            address = find("wikipedia.org", contains=True)
        if address is not None:
            x, y = address["center"]
            adb("shell", "input", "tap", str(x), str(y))
            time.sleep(2)
        elif not find(*LEFTOVER_SURFACES[0]):
            log("not found: address field")
            return
        # Otherwise the tap on the capsule's spot landed on the full bar and opened the editor.
        adb("shell", "input", "text", "zen")
        time.sleep(4)
        shot(f"address-{suffix}")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(1)
    step("address", address_editor)

    def reader():
        """Reader mode on the Zen article (board W-Reader): the article, «Reading view» over it,
        and the paper theme. The tour's own theme is picked again before leaving, so the next
        tour starts from it."""
        if not find("More options", "Другие действия"):
            # The scrolled page left the compact capsule, which exposes no label: tap its spot.
            adb("shell", "input", "tap", str(width // 2), str(int(height * 0.94)))
            time.sleep(2)
        if not tap("More options", "Другие действия"):
            log("not found: menu for reader mode")
            return
        time.sleep(2)
        if not tap_scrolling("Open Reader Studio", "Открыть режим чтения",
                             name=f"reader-menu-{suffix}"):
            return
        time.sleep(8)
        if not tap("Reading view settings", "Настройки вида для чтения"):
            save_ui(f"reader-{suffix}")
            adb("shell", "input", "keyevent", "BACK")
            return
        time.sleep(2)
        own_theme = ("Dark", "Тёмный") if suffix == "dark" else ("Light", "Светлый")
        tap(*own_theme)
        time.sleep(2)
        shot(f"reader-settings-{suffix}")
        save_ui(f"reader-settings-{suffix}")
        if tap("Paper", "Бумага"):
            time.sleep(2)
            shot(f"reader-paper-{suffix}", audit=False)
            tap(*own_theme)
            time.sleep(1)
        # Back closes the panel first, then reader mode.
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
        shot(f"reader-{suffix}")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("reader", reader)

    def https_upgrade():
        # example.com serves HTTPS, so HTTPS-only mode must upgrade this link (lock in the bar).
        open_url("http://example.com/")
        time.sleep(12)
        shot(f"https-upgrade-{suffix}")
    step("https-upgrade", https_upgrade)

    def glance():
        """Glance (board W-Glance): a long press on example.com's link opens the live preview
        card with the link actions under it. Back closes it without opening the link."""
        # Its own page, so the step does not depend on where the one before left off.
        open_url("https://example.com/")
        time.sleep(10)
        link = find("Learn more", "More information", contains=True)
        for _ in range(3):
            if link is not None:
                break
            # The link sits under the page's translations: scroll it into view.
            adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.7)),
                str(width // 2), str(int(height * 0.35)), "500")
            time.sleep(2)
            link = find("Learn more", "More information", contains=True)
        if link is None:
            save_ui(f"glance-page-{suffix}")
            log("not found: link for glance")
            return
        x, y = link["center"]
        adb("shell", "input", "swipe", str(x), str(y), str(x), str(y), "900")
        time.sleep(6)
        # The card rises over the bottom bar; caught on the way, the bar's buttons peek out.
        settle()
        shot(f"glance-{suffix}")
        save_ui(f"glance-{suffix}")
        adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("glance", glance)

    def split_view():
        """Split View (board W-Split): example.com above, the Zen page from earlier below; a tap
        on the lower card makes it the active one; the pill's close button ends it."""
        if not find("More options", "Другие действия"):
            # The scrolled page left the compact capsule, which exposes no label: tap its spot.
            adb("shell", "input", "tap", str(width // 2), str(int(height * 0.94)))
            time.sleep(2)
        if not tap("More options", "Другие действия"):
            log("not found: menu for split view")
            return
        time.sleep(2)
        if not tap_scrolling("Split View", name=f"split-menu-{suffix}"):
            adb("shell", "input", "keyevent", "BACK")
            return
        time.sleep(6)
        shot(f"split-view-{suffix}")
        save_ui(f"split-view-{suffix}")
        if tap("Bottom tab", "Нижняя вкладка", contains=True):
            time.sleep(4)
            shot(f"split-view-bottom-active-{suffix}", audit=False)
        if not tap("Close Split View", "Выйти из Split View"):
            log("split view: could not close it")
        time.sleep(2)
    step("split-view", split_view)

    def https_only_warning():
        """The HTTPS-only warning on a site without working HTTPS. Gecko shows it only when the
        upgraded request fails at the connection; a failed TLS handshake is a security error.
        So the tour tries sites until the debug log reports the warning, and logs every result."""
        for url in HTTP_ONLY_CANDIDATES:
            open_url(url)
            time.sleep(12)
            host = re.sub(r"^https?://([^/:]+).*$", r"\1", url)
            raw = adb("logcat", "-d", "-s", "VolaLoadError", capture=True, check=False) or b""
            results = [line for line in raw.decode(errors="replace").splitlines() if host in line]
            log(f"https-only probe {host}: {results[-1] if results else 'no load error'}")
            if results and "httpsOnly=true" in results[-1]:
                break
        shot(f"https-only-{suffix}")
    step("https-only", https_only_warning)

    def unknown_host_page():
        # The .invalid domain never resolves (RFC 6761): «Site not found» (Q15, board W-States).
        open_url("https://vola-tour.invalid/")
        time.sleep(8)
        shot(f"page-unknown-host-{suffix}")
    step("page-unknown-host", unknown_host_page)

    def insecure_page():
        # An expired certificate: «Insecure connection» (Q15d, board W-States).
        open_url("https://expired.badssl.com/")
        time.sleep(10)
        shot(f"page-insecure-{suffix}")
    step("page-insecure", insecure_page)

    def dangerous_site():
        # paypa1.com reads as paypal.com: the navigation stops before anything loads (Q15e,
        # board W-DangerousSite), then the tour goes back to safety.
        open_url("https://paypa1.com/")
        time.sleep(6)
        shot(f"dangerous-site-{suffix}")
        if not tap("Back to safety", "Вернуться в безопасное место"):
            adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("dangerous-site", dangerous_site)

    def offline_page():
        """No connection (board W-Offline); then the page reloads by itself once it is back."""
        airplane = ("shell", "cmd", "connectivity", "airplane-mode")
        adb(*airplane, "enable", check=False, capture=True)
        try:
            time.sleep(5)
            # A new address in every pass: Gecko would show an already visited page from cache.
            open_url(f"https://example.org/?vola-offline-{suffix}")
            time.sleep(8)
            shot(f"page-offline-{suffix}")
        finally:
            adb(*airplane, "disable", check=False, capture=True)
        time.sleep(20)
        shot(f"page-back-online-{suffix}")
    step("page-offline", offline_page)

    def workspace_locked():
        # «Workspace locked» (board W-Locked). The emulator has no biometrics to lock a workspace
        # with, so a debug-only activity shows the screen itself.
        adb("shell", "am", "start", "-n",
            f"{PACKAGE}/dev.sk2andy.materialbrowser.ui.ProfileLockedPreviewActivity",
            check=False, capture=True)
        time.sleep(3)
        shot(f"workspace-locked-{suffix}")
        if not tap("Go to another workspace", "Перейти в другое пространство"):
            adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("workspace-locked", workspace_locked)

    def download_check():
        # An app download stops at «Check the file before saving» (Q15c, board W-DownloadCheck);
        # the tour then declines, so nothing is saved.
        open_url("https://f-droid.org/F-Droid.apk")
        time.sleep(10)
        shot(f"download-check-{suffix}")
        if not tap("Don’t download", "Don't download", "Не скачивать"):
            adb("shell", "input", "keyevent", "BACK")
        time.sleep(2)
    step("download-check", download_check)

    def library():
        """History, favorites, downloads and snoozed tabs (boards W-History, W-Favorites,
        W-Downloads, W-Snoozed, W-States; Q18a and Q18b), each opened
        from the menu in its own screen; Back returns to the page. Without the menu the step
        skips rather than tapping blindly, so a missed label cannot leave an editor open. Its own
        page first: the step before may leave the bar folded into the unlabeled capsule."""
        open_url("https://example.com/")
        time.sleep(6)
        for labels, name in ((("History", "История"), "history"),
                             (("Favorites", "Избранное"), "favorites"),
                             (("Downloads", "Загрузки"), "downloads"),
                             (("Snoozed Tabs", "Отложенные вкладки"), "snoozed")):
            if not tap("More options", "Другие действия"):
                log(f"not found: menu for {name}")
                return
            time.sleep(2)
            if not tap_scrolling(*labels, name=f"{name}-menu-{suffix}"):
                adb("shell", "input", "keyevent", "BACK")
                time.sleep(1)
                continue
            time.sleep(4)
            shot(f"{name}-{suffix}")
            save_ui(f"{name}-{suffix}")
            adb("shell", "input", "keyevent", "BACK")
            time.sleep(2)
    step("library", library)

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

    def settings_top():
        """Back to the top of the settings home: its rows are found by scrolling down."""
        width, height = screen_size()
        for _ in range(3):
            adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.3)),
                str(width // 2), str(int(height * 0.8)), "200")
        time.sleep(1)

    def menu_and_settings():
        if tap("More options", "Другие действия"):
            time.sleep(2)
            shot(f"menu-{suffix}")
            if tap_scrolling("Settings", "Настройки", name=f"menu-{suffix}"):
                time.sleep(3)
                shot(f"settings-{suffix}")
                # The home's lower cards (Q16c, board W-Settings), then back to the top.
                width, height = screen_size()
                adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.75)),
                    str(width // 2), str(int(height * 0.3)), "400")
                time.sleep(1)
                shot(f"settings-more-{suffix}")
                adb("shell", "input", "swipe", str(width // 2), str(int(height * 0.3)),
                    str(width // 2), str(int(height * 0.75)), "400")
                time.sleep(1)
                # Settings search (Q16a): the magnifier in the header, then «https».
                if tap("Search settings", "Найти в настройках"):
                    time.sleep(2)
                    adb("shell", "input", "text", "https")
                    time.sleep(2)
                    shot(f"settings-search-{suffix}")
                    save_ui(f"settings-search-{suffix}")
                    # The first back hides the keyboard, the second leaves the search.
                    adb("shell", "input", "keyevent", "BACK")
                    time.sleep(1)
                    if not find("Search settings", "Найти в настройках"):
                        adb("shell", "input", "keyevent", "BACK")
                        time.sleep(2)
                # Search settings (Q16c, board W-SetSearch): engines on a card, then suggestions.
                settings_top()
                if tap_scrolling("Search", "Поиск", name=f"settings-{suffix}"):
                    time.sleep(2)
                    shot(f"search-settings-{suffix}")
                    adb("shell", "input", "keyevent", "BACK")
                    time.sleep(1)
                settings_top()
                if tap_scrolling("Appearance", "Внешний вид", name=f"settings-{suffix}"):
                    time.sleep(2)
                    shot(f"appearance-{suffix}")
                    if tap_scrolling("More settings", "Дополнительно",
                                     name=f"appearance-{suffix}"):
                        time.sleep(2)
                        shot(f"appearance-more-{suffix}", audit=False)
                    adb("shell", "input", "keyevent", "BACK")
                    time.sleep(1)
                settings_top()
                if tap_scrolling("Tabs & gestures", "Вкладки и жесты", name=f"settings-{suffix}"):
                    time.sleep(2)
                    # Tabs and gestures on cards (Q16c, board W-SetTabs).
                    shot(f"tabs-settings-{suffix}")
                    tab_archive_setting()
                    adb("shell", "input", "keyevent", "BACK")
                    time.sleep(1)
                settings_top()
                # Browser settings on cards (Q16c, board W-Settings).
                if tap_scrolling("Browser", "Браузер", name=f"settings-{suffix}"):
                    time.sleep(2)
                    shot(f"browser-settings-{suffix}")
                    adb("shell", "input", "keyevent", "BACK")
                    time.sleep(1)
                settings_top()
                # Download settings on cards (Q16c, board W-Settings).
                if tap_scrolling("Downloads", "Загрузки", name=f"settings-{suffix}"):
                    time.sleep(2)
                    shot(f"download-settings-{suffix}")
                    adb("shell", "input", "keyevent", "BACK")
                    time.sleep(1)
                settings_top()
                if tap_scrolling("Protection & data", "Защита и данные",
                                 name=f"settings-{suffix}"):
                    time.sleep(2)
                    # Protection and data on cards (Q16c, board W-SetPrivacy).
                    shot(f"protection-settings-{suffix}")
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

    def unknown_host_a11y():
        # The state page scrolls its text at 200 % and keeps the button on screen.
        open_url("https://vola-tour.invalid/")
        time.sleep(8)
        shot("page-unknown-host-a11y")
    step("page-unknown-host-a11y", unknown_host_a11y)

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
    """Switches Appearance to the Air card and shoots the edge-to-edge page."""
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
    # The layout is a pair of cards at the top of the page (board W-SetAppearance).
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
        if re.search(r"https.?only|HTTPS-Only|onLoadError|LoadURIDelegate|neverssl|httpforever", line, re.I)
    ]
    (OUT / "https-only-log.txt").write_text("\n".join(https_lines[-400:]) + "\n")
    (OUT / "tour-log.txt").write_text("\n".join(LOG) + "\n")


if __name__ == "__main__":
    main()
