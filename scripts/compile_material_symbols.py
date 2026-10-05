#!/usr/bin/env python3
"""Compiles the Material Symbols Rounded icons Vola uses into Android and Compose sources.

One manifest below lists every icon. The SVG path data is cached in material_symbols.json next
to this script, so compiling and verifying work offline; `fetch` refreshes the cache from the
google/material-design-icons repository (Apache License 2.0).

Outputs:
- app/src/main/res/drawable/<name>.xml: vector drawables for code that takes a drawable
  resource (menus built from resource ids, notifications, widgets, shortcuts);
- shared/.../ui/icons/VolaIcons.kt: ImageVectors for Compose, usable from shared code.

Usage:
  compile_material_symbols.py fetch    download missing symbols, drop unused ones
  compile_material_symbols.py          write the drawables and VolaIcons.kt
  compile_material_symbols.py verify   fail if the committed outputs are stale
"""

import json
import re
import sys
import urllib.request
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent
CACHE = Path(__file__).resolve().parent / "material_symbols.json"
DRAWABLE_DIR = ROOT / "app/src/main/res/drawable"
KOTLIN_FILE = (
    ROOT / "shared/src/commonMain/kotlin/dev/sk2andy/materialbrowser/shared/ui/icons/VolaIcons.kt"
)
SOURCE_URL = (
    "https://raw.githubusercontent.com/google/material-design-icons/master/symbols/web/"
    "{name}/materialsymbolsrounded/{name}{suffix}_24px.svg"
)
VIEWPORT = 960
BLACK = "#FF000000"
WHITE = "@android:color/white"

# Vector drawables: (resource name, symbol, filled, fill color, auto-mirrored).
# Resource names are kept from the drawables they replace, so no caller changes.
DRAWABLES = [
    ("ic_cast_connected", "cast_connected", False, WHITE, False),
    ("ic_content_copy", "content_copy", False, BLACK, False),
    ("ic_delete_outline", "delete", False, BLACK, False),
    ("ic_folder", "folder", False, BLACK, False),
    ("ic_folder_arrow_up", "drive_folder_upload", False, BLACK, False),
    ("ic_history", "history", False, BLACK, False),
    ("ic_incognito_filled", "domino_mask", True, BLACK, False),
    ("ic_incognito_outline", "domino_mask", False, BLACK, False),
    ("ic_media_playback", "play_arrow", True, "#FFFFFFFF", False),
    ("ic_pause", "pause", True, WHITE, False),
    ("ic_person_add_outline", "person_add", False, BLACK, False),
    ("ic_player_brightness", "light_mode", True, WHITE, False),
    ("ic_player_volume", "volume_up", True, WHITE, False),
    ("ic_push_pin", "push_pin", False, BLACK, False),
    ("ic_qr_code_scanner", "qr_code_scanner", False, BLACK, False),
    ("ic_reader_align_start", "format_align_left", False, BLACK, True),
    ("ic_reader_download", "download", False, WHITE, False),
    ("ic_reader_pause", "pause", True, WHITE, False),
    ("ic_reader_stop", "stop", True, WHITE, False),
    ("ic_settings", "settings", False, BLACK, False),
    ("ic_snooze", "snooze", False, "#FFFFFFFF", False),
    ("ic_switch_to_tab", "tab_move", False, BLACK, False),
    ("ic_symbol_add", "add", False, WHITE, False),
    ("ic_symbol_add_to_home_screen", "add_to_home_screen", False, BLACK, False),
    ("ic_symbol_arrow_back", "arrow_back", False, BLACK, True),
    ("ic_symbol_arrow_forward", "arrow_forward", False, BLACK, True),
    ("ic_symbol_auto_awesome", "auto_awesome", False, BLACK, False),
    ("ic_symbol_block", "block", False, WHITE, False),
    ("ic_symbol_check", "check", False, WHITE, False),
    ("ic_symbol_chevron_physical_right", "chevron_right", False, BLACK, False),
    ("ic_symbol_chevron_right", "chevron_right", False, BLACK, True),
    ("ic_symbol_close", "close", False, BLACK, False),
    ("ic_symbol_cookie", "cookie", False, WHITE, False),
    ("ic_symbol_compact_mode", "close_fullscreen", False, WHITE, False),
    ("ic_symbol_desktop", "desktop_windows", False, WHITE, False),
    ("ic_symbol_dns", "dns", False, WHITE, False),
    ("ic_symbol_extension", "extension", False, WHITE, False),
    ("ic_symbol_favorite", "favorite", False, BLACK, False),
    ("ic_symbol_favorite_filled", "favorite", True, BLACK, False),
    ("ic_symbol_find_in_page", "find_in_page", False, WHITE, False),
    ("ic_symbol_fingerprint", "fingerprint", False, WHITE, False),
    ("ic_symbol_fit_screen", "fit_screen", False, WHITE, False),
    ("ic_symbol_location_on", "location_on", False, WHITE, False),
    ("ic_symbol_mic", "mic", False, WHITE, False),
    ("ic_symbol_notifications", "notifications", False, WHITE, False),
    ("ic_symbol_open_in_new", "open_in_new", False, BLACK, True),
    ("ic_symbol_photo_camera", "photo_camera", False, WHITE, False),
    ("ic_symbol_piano", "piano", False, WHITE, False),
    ("ic_symbol_print", "print", False, BLACK, False),
    ("ic_symbol_radar", "radar", False, WHITE, False),
    ("ic_symbol_refresh", "refresh", False, BLACK, False),
    ("ic_symbol_route", "route", False, BLACK, False),
    ("ic_symbol_settings", "settings", False, BLACK, False),
    ("ic_symbol_share", "share", False, BLACK, False),
    ("ic_symbol_shield", "shield", False, WHITE, False),
    ("ic_symbol_shield_lock", "shield_lock", False, WHITE, False),
    ("ic_symbol_split_view", "splitscreen", False, WHITE, False),
    ("ic_symbol_translate", "translate", False, BLACK, False),
    ("ic_symbol_verified", "verified", False, WHITE, False),
    ("ic_symbol_vertical_scroll", "swipe_vertical", False, WHITE, False),
    ("ic_symbol_volume_off", "volume_off", False, BLACK, False),
    ("ic_symbol_zoom_in", "zoom_in", False, WHITE, False),
    ("ic_visibility", "visibility", False, BLACK, False),
    ("ic_visibility_off", "visibility_off", False, BLACK, False),
    ("ic_widget_incognito", "domino_mask", False, "@color/candy_widget_on_surface", False),
]

# Compose icons: (VolaIcons property, symbol, filled, auto-mirrored).
# Icons are outlined; the fill is kept for states and media controls, as in the v4 design.
COMPOSE_ICONS = [
    ("Add", "add", False, False),
    ("Apps", "apps", False, False),
    ("Bookmark", "bookmark", False, False),
    ("ArrowBack", "arrow_back", False, True),
    ("ArrowDropDown", "arrow_drop_down", False, False),
    ("ArrowForward", "arrow_forward", False, True),
    ("Build", "build", False, False),
    ("Check", "check", False, False),
    ("CheckCircle", "check_circle", False, False),
    ("Close", "close", False, False),
    ("CloseFullscreen", "close_fullscreen", False, False),
    ("ContentCopy", "content_copy", False, False),
    ("ContentPaste", "content_paste", False, False),
    ("Dangerous", "dangerous", False, False),
    ("Description", "description", False, False),
    ("DesktopWindows", "desktop_windows", False, False),
    ("Contrast", "contrast", False, False),
    ("DarkMode", "dark_mode", False, False),
    ("Delete", "delete", False, False),
    ("DeleteSweep", "delete_sweep", False, False),
    ("Download", "download", False, False),
    ("Edit", "edit", False, False),
    ("Error", "error", False, False),
    ("Favorite", "favorite", False, False),
    ("Fingerprint", "fingerprint", False, False),
    ("Folder", "folder", False, False),
    ("FormatAlignJustify", "format_align_justify", False, False),
    ("FormatAlignLeft", "format_align_left", False, False),
    ("GppBad", "gpp_bad", False, False),
    ("GppMaybe", "gpp_maybe", False, False),
    ("FormatSize", "format_size", False, False),
    ("Headphones", "headphones", False, False),
    ("History", "history", False, False),
    ("Home", "home", False, False),
    ("Image", "image", False, False),
    ("Info", "info", False, False),
    ("Inventory2", "inventory_2", False, False),
    ("Key", "key", False, False),
    ("KeyboardArrowDown", "keyboard_arrow_down", False, False),
    ("KeyboardArrowLeft", "keyboard_arrow_left", False, True),
    ("KeyboardArrowRight", "keyboard_arrow_right", False, True),
    ("KeyboardArrowUp", "keyboard_arrow_up", False, False),
    ("LinkOff", "link_off", False, False),
    ("LightMode", "light_mode", False, False),
    ("Lock", "lock", False, False),
    ("LockOpen", "lock_open", False, False),
    ("MoreHoriz", "more_horiz", False, False),
    ("MoreVert", "more_vert", False, False),
    ("Movie", "movie", False, False),
    ("NorthWest", "north_west", False, False),
    ("OpenInNew", "open_in_new", False, False),
    ("Palette", "palette", False, False),
    ("Pause", "pause", False, False),
    ("PictureAsPdf", "picture_as_pdf", False, False),
    ("PlayArrow", "play_arrow", False, False),
    ("PublicOff", "public_off", False, False),
    ("PushPin", "push_pin", False, False),
    ("PauseFilled", "pause", True, False),
    ("PlayArrowFilled", "play_arrow", True, False),
    ("Refresh", "refresh", False, False),
    ("Remove", "remove", False, False),
    ("Search", "search", False, False),
    ("SearchOff", "search_off", False, False),
    ("Share", "share", False, False),
    ("Snooze", "snooze", False, False),
    ("Settings", "settings", False, False),
    ("Sort", "sort", False, False),
    ("Star", "star", False, False),
    ("StopFilled", "stop", True, False),
    ("SwapVert", "swap_vert", False, False),
    ("Sync", "sync", False, False),
    ("Tab", "tab", False, False),
    ("TabGroup", "tab_group", False, False),
    ("TableChart", "table_chart", False, False),
    ("Verified", "verified", False, False),
    ("Visibility", "visibility", False, False),
    ("VisibilityOff", "visibility_off", False, False),
    ("WarningFilled", "warning", True, False),
    ("WifiOff", "wifi_off", False, False),
    ("Workspaces", "workspaces", False, False),
]

FLAG = re.compile(r"\s*,?\s*([01])")
NUMBER = re.compile(r"\s*,?\s*([-+]?(?:\d*\.\d+|\d+\.?)(?:[eE][-+]?\d+)?)")
# Arguments per command; for absolute commands, the indexes of y coordinates.
ARITY = {"m": 2, "l": 2, "t": 2, "h": 1, "v": 1, "q": 4, "s": 4, "c": 6, "a": 7, "z": 0}
Y_INDEXES = {"M": (1,), "L": (1,), "T": (1,), "V": (0,), "Q": (1, 3), "S": (1, 3),
             "C": (1, 3, 5), "A": (6,), "H": (), "Z": ()}


def cache_key(symbol, filled):
    return f"{symbol}/{'fill1' if filled else 'fill0'}"


def required_keys():
    keys = {cache_key(symbol, filled) for _, symbol, filled, _, _ in DRAWABLES}
    keys |= {cache_key(symbol, filled) for _, symbol, filled, _ in COMPOSE_ICONS}
    return sorted(keys)


def load_cache():
    return json.loads(CACHE.read_text()) if CACHE.exists() else {}


def fetch(cache):
    required = required_keys()
    for key in [key for key in cache if key not in required]:
        del cache[key]
    for key in required:
        if key in cache:
            continue
        symbol, fill = key.split("/")
        url = SOURCE_URL.format(name=symbol, suffix="_fill1" if fill == "fill1" else "")
        with urllib.request.urlopen(url, timeout=30) as response:
            svg = response.read().decode("utf-8")
        # Most symbols use the 960 grid; a few older ones are still drawn on the 24 dp grid.
        if 'viewBox="0 -960 960 960"' in svg:
            view_box = "0 -960 960 960"
        elif "viewBox" not in svg and 'height="24" width="24"' in svg:
            view_box = "0 0 24 24"
        else:
            raise ValueError(f"{key}: unexpected viewBox in {url}")
        paths = re.findall(r'<path d="([^"]+)"', svg)
        if not paths:
            raise ValueError(f"{key}: no path in {url}")
        cache[key] = {"viewBox": view_box, "d": "".join(paths)}
    CACHE.write_text(json.dumps(dict(sorted(cache.items())), indent=1) + "\n")


def format_number(value):
    text = f"{value:.3f}".rstrip("0").rstrip(".")
    return "0" if text in ("-0", "") else text


def normalize(symbol):
    """Moves a cached symbol path onto the 0 0 960 960 viewport and spells it with explicit
    separators, so the Android and Compose path parsers read it the same way."""
    path = symbol["d"]
    if symbol["viewBox"] == "0 -960 960 960":
        scale, offset = 1, VIEWPORT
    elif symbol["viewBox"] == "0 0 24 24":
        scale, offset = VIEWPORT / 24, 0
    else:
        raise ValueError(f"unsupported viewBox {symbol['viewBox']}")
    out, position, command = [], 0, None
    while position < len(path):
        if path[position] in " ,\n\t":
            position += 1
            continue
        continuation = None
        if path[position].isalpha():
            command = path[position]
            position += 1
            if command in "Zz":
                out.append("Z")
                continue
            # The first moveto of a path is absolute even when written as "m"; the pairs after
            # it stay relative linetos.
            if command == "m" and not out:
                command, continuation = "M", "l"
        elif command is None:
            raise ValueError(f"path starts with a number: {path[:20]}")
        arity = ARITY[command.lower()]
        args = []
        for index in range(arity):
            pattern = FLAG if command in "Aa" and index in (3, 4) else NUMBER
            match = pattern.match(path, position)
            if not match:
                raise ValueError(f"bad arguments for {command} at {position}: {path[position:position + 20]}")
            args.append(float(match.group(1)))
            position = match.end()
        # Arc rotation and flags are not lengths; everything else scales.
        lengths = [index for index in range(arity) if not (command in "Aa" and index in (2, 3, 4))]
        for index in lengths:
            args[index] *= scale
        if command.isupper():
            for index in Y_INDEXES[command]:
                args[index] += offset
        out.append(command + " ".join(format_number(arg) for arg in args))
        # A moveto followed by more pairs continues as lineto.
        if continuation:
            command = continuation
        elif command == "M":
            command = "L"
        elif command == "m":
            command = "l"
    return " ".join(out)


def drawable_xml(symbol, filled, color, auto_mirrored, path):
    mirrored = '\n    android:autoMirrored="true"' if auto_mirrored else ""
    variant = "filled" if filled else "outlined"
    return f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Generated by scripts/compile_material_symbols.py, do not edit.
     Material Symbols Rounded "{symbol}", {variant} (Apache License 2.0). -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    android:width="24dp"
    android:height="24dp"
    android:viewportWidth="{VIEWPORT}"
    android:viewportHeight="{VIEWPORT}"{mirrored}>
    <path
        android:fillColor="{color}"
        android:pathData="{path}" />
</vector>
"""


def kotlin_source(cache):
    lines = [
        "// Generated by scripts/compile_material_symbols.py, do not edit.",
        "// Material Symbols Rounded (Apache License 2.0): weight 400, grade 0, optical size 24.",
        "package dev.sk2andy.materialbrowser.shared.ui.icons",
        "",
        "import androidx.compose.ui.graphics.vector.ImageVector",
        "",
        "/**",
        " * The Material Symbols Rounded icons of the Vola interface. Icons are outlined; names that",
        " * end in Filled carry the fill for selected states and media controls.",
        " */",
        "object VolaIcons {",
    ]
    for name, symbol, filled, auto_mirrored in COMPOSE_ICONS:
        path = normalize(cache[cache_key(symbol, filled)])
        lines.append(f"    val {name}: ImageVector by lazy {{")
        lines.append("        materialSymbol(")
        lines.append(f'            name = "{name}",')
        if auto_mirrored:
            lines.append("            autoMirror = true,")
        lines.append(f'            pathData = "{path}",')
        lines.append("        )")
        lines.append("    }")
        lines.append("")
    lines.append("    /** Every icon above, for previews and tests. */")
    lines.append("    val all: List<ImageVector>")
    lines.append("        get() = listOf(")
    for name, _, _, _ in COMPOSE_ICONS:
        lines.append(f"            {name},")
    lines.append("        )")
    lines.append("}")
    return "\n".join(lines) + "\n"


def outputs(cache):
    missing = [key for key in required_keys() if key not in cache]
    if missing:
        raise SystemExit(f"missing from the cache, run fetch: {', '.join(missing)}")
    files = {}
    for name, symbol, filled, color, auto_mirrored in DRAWABLES:
        path = normalize(cache[cache_key(symbol, filled)])
        files[DRAWABLE_DIR / f"{name}.xml"] = drawable_xml(symbol, filled, color, auto_mirrored, path)
    files[KOTLIN_FILE] = kotlin_source(cache)
    return files


def main(argv):
    mode = argv[1] if len(argv) > 1 else "write"
    cache = load_cache()
    if mode == "fetch":
        fetch(cache)
        cache = load_cache()
        mode = "write"
    files = outputs(cache)
    if mode == "verify":
        stale = [str(path.relative_to(ROOT)) for path, text in files.items()
                 if not path.exists() or path.read_text() != text]
        if stale:
            raise SystemExit("stale, run scripts/compile_material_symbols.py: " + ", ".join(stale))
        return
    if mode != "write":
        raise SystemExit(__doc__)
    for path, text in files.items():
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(text)


if __name__ == "__main__":
    main(sys.argv)
