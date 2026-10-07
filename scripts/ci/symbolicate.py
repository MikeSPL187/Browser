#!/usr/bin/env python3
"""Symbolicates the native frames of a Gecko crash with Mozilla's symbol server.

Reads any text holding Android backtrace lines - a logcat crash buffer, a tombstone, or a report
from Vola's crash journal (#126) - and prints each backtrace with function, file and line:

  scripts/ci/symbolicate.py crash.txt
  adb logcat -d -b crash | scripts/ci/symbolicate.py

Only libraries Mozilla publishes symbols for (libxul.so, libmozglue.so, ...) get names; Android's
own frames stay as they are. Nothing but library names, build ids and offsets leaves the machine.
"""

import json
import re
import sys
import urllib.request

ENDPOINT = "https://symbolication.services.mozilla.com/symbolicate/v5"
FRAME = re.compile(r"#(\d+) pc ([0-9a-f]+)\s+\S*?(lib[\w.+-]+\.so)\b.*?\(BuildId: ([0-9a-f]+)\)")


def debug_id(build_id):
    """Breakpad's debug id: the first 16 bytes of the ELF build id in GUID byte order, then "0"."""
    raw = bytes.fromhex(build_id.ljust(32, "0")[:32])
    return (raw[3::-1] + raw[5:3:-1] + raw[7:5:-1] + raw[8:]).hex().upper() + "0"


def backtraces(text):
    """Each run of frames numbered from #00, as [(index, pc, library, build id)]."""
    traces, current, seen = [], [], set()
    for line in text.splitlines():
        match = FRAME.search(line)
        if not match:
            continue
        index = int(match.group(1))
        if index == 0 or index in seen:
            if current:
                traces.append(current)
            current, seen = [], set()
        seen.add(index)
        current.append((index, int(match.group(2), 16), match.group(3), match.group(4)))
    if current:
        traces.append(current)
    return traces


def request_body(frames):
    modules, stack = {}, []
    for _, pc, library, build_id in frames:
        index = modules.setdefault((library, debug_id(build_id)), len(modules))
        stack.append([index, pc])
    memory_map = [list(key) for key in sorted(modules, key=modules.get)]
    return {"jobs": [{"memoryMap": memory_map, "stacks": [stack]}]}


def post(body, timeout=120):
    request = urllib.request.Request(
        ENDPOINT,
        data=json.dumps(body).encode(),
        headers={"Content-Type": "application/json"},
    )
    with urllib.request.urlopen(request, timeout=timeout) as response:
        return json.load(response)


def render(frames, result):
    """The frames as text: named where the server knew the library, as given where it did not."""
    lines = []
    named = result["results"][0]["stacks"][0] if result else []
    for position, (index, pc, library, _) in enumerate(frames):
        item = named[position] if position < len(named) else {}
        function = item.get("function")
        if function:
            where = f" {item['file']}:{item.get('line', '')}" if item.get("file") else ""
            offset = item.get("function_offset", "")
            lines.append(f"#{index:02d} {library} {function}+{offset}{where}")
        else:
            lines.append(f"#{index:02d} {library} pc {pc:08x}")
    return lines


def symbolicate(text, fetch=post):
    """Every backtrace in [text], symbolicated; a server failure is reported, not raised."""
    blocks = []
    for frames in backtraces(text):
        try:
            result = fetch(request_body(frames))
            note = None
        except Exception as error:  # the frames stay readable without names
            result, note = None, f"symbolication failed: {error}"
        lines = render(frames, result)
        if note:
            lines.insert(0, note)
        blocks.append(lines)
    return blocks


def main(argv):
    text = open(argv[0], errors="replace").read() if argv else sys.stdin.read()
    blocks = symbolicate(text)
    if not blocks:
        print("no native backtrace with build ids found")
        return 1
    for number, lines in enumerate(blocks, start=1):
        print(f"--- backtrace {number}")
        print("\n".join(lines))
    return 0


if __name__ == "__main__":
    sys.exit(main(sys.argv[1:]))
