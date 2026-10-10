#!/usr/bin/env python3
"""Records short videos of key scenarios on a running emulator for UI pull requests (plan v5, V4).

Usage: record_ui_video.py <package> <output-dir>

The owner reviews pull requests from a phone, where screenshots cannot show motion. For each
scenario the script starts `screenrecord` on the device, drives the scenario with adb input,
stops the recording with SIGINT (so the mp4 is finalized), pulls it and tiles eight frames into a
contact sheet with ffmpeg. summary.md in the output directory lists the scenarios, their
durations and a checklist to copy into the pull request description.

System animations stay at their defaults: the videos exist to show motion. Every scenario is best
effort: a failed one is noted in the summary and the next one still runs. The script fails only
when no video was recorded at all.

The pure helpers are unit-tested in test_record_ui_video.py.
"""

import os
import re
import shutil
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ElementTree
from pathlib import Path

import page_probe

BIT_RATE = 6_000_000
SIZE = "720x1600"
# screenrecord stops by itself after 180 s; scenarios take under a minute, this only caps a hang.
TIME_LIMIT_S = 90
SHEET_COLUMNS = 4
SHEET_ROWS = 2
SHEET_FRAME_WIDTH = 240
DEFAULT_SCREEN = (1080, 2400)

FIRST_RUN_START = ("Get started", "Начать")
FIRST_RUN_NEXT = ("Next", "Далее")
FIRST_RUN_SKIP = ("Skip", "Пропустить")
WHATS_NEW_CLOSE = ("Explore Vola", "К браузеру")
MENU_LABELS = ("More options", "Другие действия")

# (id, title, what to check on the phone). The id names the files; titles and checks are Russian
# because the owner reads the summary and copies the checklist into the pull request.
SCENARIOS = (
    ("cold-start", "Холодный старт",
     "Холодный старт: от нажатия на значок до первого кадра нет белой вспышки и рывка панели."),
    ("scroll", "Прокрутка длинной страницы",
     "Прокрутка: при быстрой прокрутке вниз панель плавно уходит, вверх — возвращается без скачка."),
    ("tab-swipe", "Свайп вкладок по нижней панели",
     "Свайп по нижней панели влево и вправо переключает вкладки, страница едет за пальцем."),
    ("overview", "Обзор вкладок",
     "Свайп вверх от панели открывает обзор вкладок, «Назад» закрывает его без рывка."),
)


def remote_path(scenario):
    return f"/sdcard/vola-{scenario}.mp4"


def screenrecord_command(path, size=SIZE, bit_rate=BIT_RATE, time_limit=TIME_LIMIT_S):
    """The adb command that records the screen to [path]; [size] None keeps the native size."""
    command = ["adb", "shell", "screenrecord", "--bit-rate", str(bit_rate)]
    if size:
        command += ["--size", size]
    return command + ["--time-limit", str(time_limit), path]


def parse_screen_size(output):
    """(width, height) from `wm size`; an override (a changed resolution) wins over the panel."""
    override = re.search(r"Override size:\s*(\d+)x(\d+)", output)
    match = override or re.search(r"(\d+)x(\d+)", output)
    return (int(match.group(1)), int(match.group(2))) if match else DEFAULT_SCREEN


def parse_nodes(raw):
    """Nodes of a uiautomator dump as {text, desc, center}; [] when the dump is not XML."""
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


def find_node(nodes, labels, contains=False):
    """The first node whose text or description is one of [labels] (or contains one)."""
    for node in nodes:
        for value in (node["text"], node["desc"]):
            for label in labels:
                if value == label or (contains and value and label.lower() in value.lower()):
                    return node
    return None


def bar_y(nodes, height):
    """Where the bottom bar is: the menu button sits on it; without it, near the bottom edge."""
    menu = find_node(nodes, MENU_LABELS)
    if menu is not None and menu["center"][1] > height // 2:
        return menu["center"][1]
    return int(height * 0.95)


def fling(width, height, down):
    """`input swipe` arguments of a quick flick: the finger moves up to scroll the page down."""
    x = width // 2
    top, bottom = int(height * 0.3), int(height * 0.75)
    start, end = (bottom, top) if down else (top, bottom)
    return ["shell", "input", "swipe", str(x), str(start), str(x), str(end), "150"]


def bar_swipe(width, y, to_left):
    """`input swipe` arguments of a sideways swipe along the bottom bar."""
    left, right = int(width * 0.28), int(width * 0.72)
    start, end = (right, left) if to_left else (left, right)
    return ["shell", "input", "swipe", str(start), str(y), str(end), str(y), "220"]


def overview_swipe(width, height, y):
    """`input swipe` arguments of the swipe up from the bar that opens the tab overview."""
    x = width // 2
    return ["shell", "input", "swipe", str(x), str(y), str(x), str(int(height * 0.35)), "350"]


def scroll_page(pages):
    """The long page for the scroll scenario: scroll.html when the branch has it."""
    return "scroll.html" if (Path(pages) / "scroll.html").is_file() else "touch.html"


def launcher_component(output):
    """package/activity from `cmd package resolve-activity --brief`, or None."""
    lines = [line.strip() for line in output.splitlines() if line.strip()]
    return lines[-1] if lines and "/" in lines[-1] else None


def parse_total_time(output):
    """TotalTime in ms from `am start -W`, or None."""
    match = re.search(r"TotalTime:\s*(\d+)", output)
    return int(match.group(1)) if match else None


def parse_duration(output):
    """Seconds from `ffprobe -show_entries format=duration -of csv=p=0`, or None."""
    try:
        value = float(output.strip().splitlines()[0])
    except (IndexError, ValueError):
        return None
    return value if value > 0 else None


def sample_times(duration, count=SHEET_COLUMNS * SHEET_ROWS):
    """[count] moments spread evenly over the video, each in the middle of its slice."""
    if not duration or duration <= 0 or count <= 0:
        return []
    return [round(duration * (index + 0.5) / count, 3) for index in range(count)]


def frame_command(video, seconds, target):
    return ["ffmpeg", "-y", "-loglevel", "error", "-ss", f"{seconds:.3f}", "-i", str(video),
            "-frames:v", "1", str(target)]


def tile_command(pattern, target, columns=SHEET_COLUMNS, rows=SHEET_ROWS,
                 frame_width=SHEET_FRAME_WIDTH):
    """Tiles numbered frames (frame-1.png, …) into one image; a short video leaves gaps."""
    return ["ffmpeg", "-y", "-loglevel", "error", "-framerate", "1", "-start_number", "1",
            "-i", str(pattern),
            "-vf", f"scale={frame_width}:-2,tile={columns}x{rows}:padding=8:margin=8:color=gray",
            "-frames:v", "1", str(target)]


def summary_markdown(results, run_number=None):
    """The job summary: one row per scenario and the checklist for the pull request."""
    title = "### Видео сценариев (V4)"
    if run_number:
        title += f" — запуск {run_number}"
    lines = [title, "",
             f"Артефакт `ui-video-{run_number or '<номер>'}`: mp4 каждого сценария и лист из "
             f"{SHEET_COLUMNS * SHEET_ROWS} кадров (PNG). Системные анимации включены, касания "
             "видны точками.", "",
             "Эмулятор CI рисует без видеокарты, поэтому рывки на видео — от эмулятора, а не от "
             "приложения: видео показывает логику движения, плавность проверяется на телефоне.", "",
             "| Сценарий | Длительность | Файлы | Заметки |", "|---|---|---|---|"]
    for result in results:
        duration = f"{result['duration']:.1f} с" if result.get("duration") else "—"
        files = ", ".join(f"`{name}`" for name in result.get("files", ())) or "не записано"
        lines.append(f"| {result['title']} | {duration} | {files} | {result.get('note') or ''} |")
    lines += ["", "**Что потрогать на телефоне** (скопируйте в описание PR и отметьте):", "",
              "```markdown"]
    lines += [f"- [ ] {check}" for _, _, check in SCENARIOS]
    lines += ["- [ ] Тёмная тема и русский язык: то же самое, без обрезанных подписей.", "```", ""]
    return "\n".join(lines)


# ---------------------------------------------------------------------------------------------
# Device side: everything below talks to adb and is exercised only on the emulator.


class Recorder:
    def __init__(self, package, out):
        self.package = package
        self.out = Path(out)
        self.out.mkdir(parents=True, exist_ok=True)
        self.log_lines = []
        self.size = DEFAULT_SCREEN

    def log(self, message):
        print(message, flush=True)
        self.log_lines.append(message)

    def adb(self, *args, timeout=60):
        """Output of an adb command as text; a failure is logged, never raised."""
        try:
            result = subprocess.run(["adb", *args], stdout=subprocess.PIPE,
                                    stderr=subprocess.STDOUT, timeout=timeout, check=False)
        except subprocess.TimeoutExpired:
            self.log(f"adb {' '.join(args[:3])}: timed out")
            return ""
        return result.stdout.decode("utf-8", "replace")

    def nodes(self):
        self.adb("shell", "rm", "-f", "/sdcard/vola-ui.xml")
        self.adb("shell", "uiautomator", "dump", "/sdcard/vola-ui.xml")
        raw = subprocess.run(["adb", "exec-out", "cat", "/sdcard/vola-ui.xml"],
                             stdout=subprocess.PIPE, check=False, timeout=60).stdout
        return parse_nodes(raw)

    def tap(self, labels):
        node = find_node(self.nodes(), labels)
        if node is None:
            return False
        x, y = node["center"]
        self.adb("shell", "input", "tap", str(x), str(y))
        self.log(f"tap {labels[0]!r} at {x},{y}")
        return True

    def open_url(self, url):
        # A link from another app opens in a new tab.
        self.adb("shell", "am", "start", "-a", "android.intent.action.VIEW", "-d", url,
                 self.package)

    def prepare(self):
        self.size = parse_screen_size(self.adb("shell", "wm", "size"))
        for setting in ("window_animation_scale", "transition_animation_scale",
                        "animator_duration_scale"):
            self.adb("shell", "settings", "put", "global", setting, "1")
        # Dots where the finger is, so a swipe in the video reads as a swipe.
        self.adb("shell", "settings", "put", "system", "show_touches", "1")
        self.adb("shell", "cmd", "uimode", "night", "no")
        self.adb("shell", "monkey", "-p", self.package, "-c",
                 "android.intent.category.LAUNCHER", "1")
        time.sleep(15)
        self.dismiss_first_run()

    def dismiss_first_run(self):
        """Welcome, setup and the gesture lesson; then "What's new" if it shows."""
        for _ in range(6):
            nodes = self.nodes()
            if find_node(nodes, FIRST_RUN_START):
                self.tap(FIRST_RUN_START)
                time.sleep(2)
                self.tap(FIRST_RUN_NEXT)
                time.sleep(2)
                self.tap(FIRST_RUN_SKIP)
            elif find_node(nodes, FIRST_RUN_SKIP):
                self.tap(FIRST_RUN_SKIP)
            elif find_node(nodes, WHATS_NEW_CLOSE):
                self.tap(WHATS_NEW_CLOSE)
            else:
                return
            time.sleep(3)

    def start_recording(self, scenario):
        """Starts screenrecord; a size the encoder refuses falls back to the native size."""
        path = remote_path(scenario)
        self.adb("shell", "rm", "-f", path)
        for size in (SIZE, None):
            process = subprocess.Popen(screenrecord_command(path, size=size),
                                       stdin=subprocess.DEVNULL, stdout=subprocess.PIPE,
                                       stderr=subprocess.STDOUT)
            # The encoder needs a moment before the first frame.
            time.sleep(1.5)
            if process.poll() is None:
                return process
            output = process.stdout.read().decode("utf-8", "replace").strip()
            self.log(f"{scenario}: screenrecord --size {size} exited: {output[:200]}")
        return None

    def stop_recording(self, scenario, process):
        """SIGINT lets screenrecord finish the mp4; then the file is pulled."""
        time.sleep(1)
        self.adb("shell", "pkill", "-INT", "screenrecord")
        try:
            process.wait(timeout=20)
        except subprocess.TimeoutExpired:
            self.log(f"{scenario}: screenrecord did not stop, killing it")
            self.adb("shell", "pkill", "-KILL", "screenrecord")
            process.kill()
        # The file is complete once its size stops changing.
        previous = None
        for _ in range(10):
            size = self.adb("shell", "stat", "-c", "%s", remote_path(scenario)).strip()
            if size and size == previous:
                break
            previous = size
            time.sleep(1)
        local = self.out / f"{scenario}.mp4"
        self.adb("pull", remote_path(scenario), str(local), timeout=120)
        return local if local.is_file() and local.stat().st_size > 0 else None

    def record(self, scenario, title, drive):
        result = {"id": scenario, "title": title, "files": [], "note": ""}
        process = self.start_recording(scenario)
        if process is None:
            result["note"] = "screenrecord не запустился"
            return result
        started = time.monotonic()
        try:
            result["note"] = drive() or ""
        except Exception as error:  # noqa: BLE001 - one broken scenario never hides the rest
            self.log(f"{scenario}: failed: {error}")
            result["note"] = f"сбой сценария: {error}"
        wall = time.monotonic() - started
        video = self.stop_recording(scenario, process)
        if video is None:
            result["note"] = (result["note"] + "; файл не получен").lstrip("; ")
            return result
        result["files"].append(video.name)
        result["duration"] = self.duration(video) or wall
        sheet = self.contact_sheet(video, result["duration"])
        if sheet is not None:
            result["files"].append(sheet.name)
        self.log(f"{scenario}: {result['duration']:.1f} s, {', '.join(result['files'])}")
        return result

    def duration(self, video):
        if shutil.which("ffprobe") is None:
            return None
        output = subprocess.run(["ffprobe", "-v", "error", "-show_entries", "format=duration",
                                 "-of", "csv=p=0", str(video)], stdout=subprocess.PIPE,
                                check=False, timeout=60).stdout.decode("utf-8", "replace")
        return parse_duration(output)

    def contact_sheet(self, video, duration):
        if shutil.which("ffmpeg") is None:
            self.log("ffmpeg not found: no contact sheets")
            return None
        target = self.out / f"{video.stem}-frames.png"
        with tempfile.TemporaryDirectory() as frames:
            count = 0
            for seconds in sample_times(duration):
                frame = Path(frames) / f"frame-{count + 1}.png"
                subprocess.run(frame_command(video, seconds, frame), check=False, timeout=60)
                if frame.is_file():
                    count += 1
            if count == 0:
                self.log(f"{video.stem}: no frames for the contact sheet")
                return None
            subprocess.run(tile_command(Path(frames) / "frame-%d.png", target), check=False,
                           timeout=120)
        return target if target.is_file() else None

    # Scenarios -----------------------------------------------------------------------------

    def cold_start(self):
        """From the launcher to the first frame, as a person opens Vola."""
        component = launcher_component(self.adb("shell", "cmd", "package", "resolve-activity",
                                                 "--brief", "-c",
                                                 "android.intent.category.LAUNCHER", self.package))
        if component is None:
            raise RuntimeError("launcher activity not found")
        output = self.adb("shell", "am", "start", "-W", "-n", component, timeout=90)
        time.sleep(6)
        total = parse_total_time(output)
        return f"am start -W: {total} мс" if total is not None else "am start -W без TotalTime"

    def scroll(self):
        width, height = self.size
        for _ in range(3):
            self.adb(*fling(width, height, down=True))
            time.sleep(0.8)
        time.sleep(1.5)
        for _ in range(3):
            self.adb(*fling(width, height, down=False))
            time.sleep(0.8)
        time.sleep(2)
        return ""

    def tab_swipe(self):
        width, height = self.size
        y = bar_y(self.nodes(), height)
        for to_left in (False, True, True, False):
            self.adb(*bar_swipe(width, y, to_left))
            time.sleep(2)
        return f"панель на y={y}"

    def overview(self):
        width, height = self.size
        y = bar_y(self.nodes(), height)
        self.adb(*overview_swipe(width, height, y))
        time.sleep(3)
        self.adb("shell", "input", "keyevent", "BACK")
        time.sleep(2.5)
        return ""

    def run(self):
        page_probe.serve()
        self.adb("reverse", f"tcp:{page_probe.PORT}", f"tcp:{page_probe.PORT}")
        self.prepare()
        titles = {scenario: title for scenario, title, _ in SCENARIOS}
        results = []

        # Cold start begins on the launcher with Vola stopped.
        self.adb("shell", "am", "force-stop", self.package)
        self.adb("shell", "input", "keyevent", "HOME")
        time.sleep(3)
        results.append(self.record("cold-start", titles["cold-start"], self.cold_start))

        long_page = scroll_page(page_probe.PAGES)
        self.open_url(f"{page_probe.BASE_URL}/{long_page}")
        time.sleep(6)
        results.append(self.record("scroll", titles["scroll"], self.scroll))

        # A second tab, so the swipe has somewhere to go.
        self.open_url(f"{page_probe.BASE_URL}/form.html")
        time.sleep(6)
        results.append(self.record("tab-swipe", titles["tab-swipe"], self.tab_swipe))
        results.append(self.record("overview", titles["overview"], self.overview))

        self.adb("shell", "settings", "put", "system", "show_touches", "0")
        crashes = self.adb("logcat", "-d", "-b", "crash")
        (self.out / "crash-log.txt").write_text(crashes or "No crashes recorded.\n")
        (self.out / "record-log.txt").write_text("\n".join(self.log_lines) + "\n")
        return results


def main():
    if len(sys.argv) != 3:
        print(__doc__.splitlines()[2], file=sys.stderr)
        return 2
    recorder = Recorder(sys.argv[1], sys.argv[2])
    results = recorder.run()
    run_number = os.environ.get("GITHUB_RUN_NUMBER")
    summary = summary_markdown(results, run_number)
    (recorder.out / "summary.md").write_text(summary)
    print(summary)
    if not any(result["files"] for result in results):
        print("No video was recorded.", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    sys.exit(main())
