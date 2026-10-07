#!/usr/bin/env python3
"""Measurements of the page itself for the emulator tour (#123, screen 5).

The tour serves scripts/ci/pages/ on the host and forwards the port to the emulator, so Vola
opens the probe pages at http://localhost. The touch probe draws full-width bands whose red
channel encodes their index; the tour finds where a band is drawn on screen in a raw screenshot,
taps its middle and compares it with where the page says the tap landed. A mismatch is the
"tap lands on the wrong element" bug the owner saw in the Air layout and Compact Mode.

The pure helpers are unit-tested in test_page_probe.py.
"""

import functools
import http.server
import re
import struct
import threading
from pathlib import Path

PAGES = Path(__file__).resolve().parent / "pages"
PORT = 8765
BASE_URL = f"http://localhost:{PORT}"

BAND_GREEN = 180
BAND_BLUE = 60
BAND_RED_BASE = 20
BAND_RED_STEP = 6
BAND_COUNT = 40
COLOR_TOLERANCE = 2
MIN_VISIBLE_BAND_PX = 60

RESULT = re.compile(r"PROBE tap (\d+) y (-?\d+) of (\d+)")


def serve():
    """Serves the probe pages from a daemon thread; returns the server."""
    handler = functools.partial(_QuietHandler, directory=str(PAGES))
    server = http.server.ThreadingHTTPServer(("127.0.0.1", PORT), handler)
    threading.Thread(target=server.serve_forever, daemon=True).start()
    return server


class _QuietHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, format, *args):  # noqa: A002 - the base class names it so
        pass


def parse_raw_screencap(data):
    """(width, height, pixels) of `adb exec-out screencap` without -p: a header of width, height
    and format (and, since Android 11, a color space), then RGBA bytes."""
    if len(data) < 12:
        raise ValueError("screencap too short")
    width, height, _ = struct.unpack_from("<III", data, 0)
    pixels = width * height * 4
    header = len(data) - pixels
    if header not in (12, 16):
        raise ValueError(f"unexpected screencap layout: {len(data)} bytes for {width}x{height}")
    return width, height, memoryview(data)[header:]


def band_at(red, green, blue):
    """The band index a pixel belongs to, or None."""
    if abs(green - BAND_GREEN) > COLOR_TOLERANCE or abs(blue - BAND_BLUE) > COLOR_TOLERANCE:
        return None
    offset = red - BAND_RED_BASE
    index = round(offset / BAND_RED_STEP)
    if abs(offset - index * BAND_RED_STEP) > COLOR_TOLERANCE or not 0 <= index < BAND_COUNT:
        return None
    return index


def band_runs(width, height, pixels, x):
    """{band: (first_y, last_y)} for the bands drawn in column [x]: where each band is on screen."""
    runs = {}
    for y in range(height):
        offset = (y * width + x) * 4
        band = band_at(pixels[offset], pixels[offset + 1], pixels[offset + 2])
        if band is None:
            continue
        first, _ = runs.get(band, (y, y))
        runs[band] = (first, y)
    return runs


def target_band(runs, height):
    """The band nearest the middle of the screen whose neighbours are drawn too, so its top and
    bottom edges are on screen: (band, first_y, last_y)."""
    whole = [
        (band, first, last) for band, (first, last) in runs.items()
        if band - 1 in runs and band + 1 in runs and last - first + 1 >= MIN_VISIBLE_BAND_PX
    ]
    if not whole:
        return None
    return min(whole, key=lambda run: abs((run[1] + run[2]) / 2 - height / 2))


def parse_result(text):
    """(band, y inside the band, band height) the page reported, both in device pixels."""
    match = RESULT.search(text or "")
    if match is None:
        return None
    return int(match.group(1)), int(match.group(2)), int(match.group(3))


def touch_offset(tap_y, band, first_y, last_y, reported):
    """How many device pixels the page placed the tap away from where the finger was.

    [band] is drawn from [first_y] to [last_y] on screen and the finger was at [tap_y]. Both
    positions are compared from the top of the document, so a tap that landed on another band
    still measures. Positive: the page received it lower than the finger; negative: higher (the
    owner's case: "Watch" pressed, a chip above it selected). None when nothing was reported.
    """
    if reported is None:
        return None
    got, y_in_band, page_band_height = reported
    screen_band_height = last_y - first_y + 1
    if screen_band_height <= 0 or page_band_height <= 0:
        return None
    # A page zoom changes the band's height on screen; measure in the page's device pixels.
    scale = page_band_height / screen_band_height
    finger = band * page_band_height + (tap_y - first_y) * scale
    page = got * page_band_height + y_in_band
    return round(page - finger)


GFXINFO_FIELDS = {
    "frames": re.compile(r"Total frames rendered:\s*(\d+)"),
    "janky": re.compile(r"Janky frames:\s*(\d+)"),
    "p50": re.compile(r"50th percentile:\s*(\d+)ms"),
    "p90": re.compile(r"90th percentile:\s*(\d+)ms"),
    "p95": re.compile(r"95th percentile:\s*(\d+)ms"),
    "p99": re.compile(r"99th percentile:\s*(\d+)ms"),
}


def parse_gfxinfo(text):
    """Frame statistics of `dumpsys gfxinfo <package>`: the first (app-wide) summary only."""
    stats = {}
    for name, pattern in GFXINFO_FIELDS.items():
        match = pattern.search(text or "")
        if match is not None:
            stats[name] = int(match.group(1))
    return stats


def describe_frames(stats):
    """One log line: '42 frames, 7 janky (17%), p50 9 ms, p90 21 ms, p99 48 ms'."""
    if "frames" not in stats:
        return "no frame statistics"
    frames = stats["frames"]
    janky = stats.get("janky", 0)
    share = round(100 * janky / frames) if frames else 0
    percentiles = ", ".join(
        f"{name} {stats[name]} ms" for name in ("p50", "p90", "p99") if name in stats
    )
    return f"{frames} frames, {janky} janky ({share}%), {percentiles}"


def card_spread(width, height, pixels, step=16):
    """Brightness spread (standard deviation, 0–255) of the page card area: about 0 for an
    empty card, tens for a page with text (#123, H7: a loaded page showed an empty card)."""
    top, bottom = int(height * 0.08), int(height * 0.85)
    left, right = min(40, width // 4), max(width - 40, width * 3 // 4)
    values = []
    for y in range(top, bottom, step):
        row = y * width
        for x in range(left, right, step):
            offset = (row + x) * 4
            red, green, blue = pixels[offset], pixels[offset + 1], pixels[offset + 2]
            values.append(0.299 * red + 0.587 * green + 0.114 * blue)
    if not values:
        return 0.0
    mean = sum(values) / len(values)
    return round((sum((value - mean) ** 2 for value in values) / len(values)) ** 0.5, 1)
