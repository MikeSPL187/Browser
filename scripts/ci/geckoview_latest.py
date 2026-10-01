#!/usr/bin/env python3
"""Finds the newest stable GeckoView release and compares it with the version Vola builds against.

GeckoView release versions on maven.mozilla.org are the Firefox version followed by the build id:
`156.0.20260921121718` for a major release, `156.0.1.20260925103000` for a dot release. Beta and
nightly builds live in other artifacts (geckoview-beta, geckoview-nightly), so every well-formed
version in this artifact is a stable release; anything else is ignored.

Usage:
  geckoview_latest.py check [--metadata FILE]   print current=, latest=, display=, update= lines
  geckoview_latest.py apply VERSION             write VERSION into gradle/libs.versions.toml

`check` prints in the key=value form that GitHub Actions reads from $GITHUB_OUTPUT.
"""

import argparse
import re
import sys
import urllib.request
import xml.etree.ElementTree as ElementTree
from pathlib import Path

ROOT = Path(__file__).resolve().parent.parent.parent
CATALOG = ROOT / "gradle/libs.versions.toml"
METADATA_URL = (
    "https://maven.mozilla.org/maven2/org/mozilla/geckoview/geckoview/maven-metadata.xml"
)

VERSION_PATTERN = re.compile(r"^(\d+(?:\.\d+){1,3})\.(\d{14})$")
CATALOG_PATTERN = re.compile(r'^(geckoview\s*=\s*")([^"]+)(")', re.MULTILINE)


def parse_version(value):
    """Returns a sortable (release, build id) pair, or None for a version that is not a release."""
    match = VERSION_PATTERN.match(value.strip())
    if not match:
        return None
    release = tuple(int(part) for part in match.group(1).split("."))
    # 156.0 and 156.0.0 are the same release; pad so dot releases sort after the major one.
    release += (0,) * (3 - len(release))
    return release, int(match.group(2))


def display_version(value):
    """The Firefox version people know, without the build id: 156.0.1.20260925103000 -> 156.0.1."""
    return value.rsplit(".", 1)[0]


def versions_from_metadata(xml_text):
    root = ElementTree.fromstring(xml_text)
    return [node.text.strip() for node in root.iter("version") if node.text]


def latest_release(versions):
    releases = [(parse_version(version), version) for version in versions]
    releases = [(key, version) for key, version in releases if key is not None]
    if not releases:
        raise ValueError("no GeckoView release versions in the metadata")
    return max(releases)[1]


def current_version(catalog_text):
    match = CATALOG_PATTERN.search(catalog_text)
    if not match:
        raise ValueError("no geckoview version in the version catalog")
    return match.group(2)


def is_newer(candidate, current):
    current_key = parse_version(current)
    if current_key is None:
        raise ValueError(f"the catalog version {current!r} is not a GeckoView release version")
    return parse_version(candidate) > current_key


def with_version(catalog_text, version):
    if parse_version(version) is None:
        raise ValueError(f"{version!r} is not a GeckoView release version")
    return CATALOG_PATTERN.sub(lambda match: match.group(1) + version + match.group(3), catalog_text, 1)


def read_metadata(path):
    if path:
        return Path(path).read_text(encoding="utf-8")
    with urllib.request.urlopen(METADATA_URL, timeout=30) as response:
        return response.read().decode("utf-8")


def check(metadata_path):
    current = current_version(CATALOG.read_text(encoding="utf-8"))
    latest = latest_release(versions_from_metadata(read_metadata(metadata_path)))
    print(f"current={current}")
    print(f"latest={latest}")
    print(f"display={display_version(latest)}")
    print(f"update={'true' if is_newer(latest, current) else 'false'}")


def apply(version):
    CATALOG.write_text(with_version(CATALOG.read_text(encoding="utf-8"), version), encoding="utf-8")


def main(argv):
    parser = argparse.ArgumentParser(description=__doc__.splitlines()[0])
    commands = parser.add_subparsers(dest="command", required=True)
    check_parser = commands.add_parser("check")
    check_parser.add_argument("--metadata", help="read maven-metadata.xml from a file")
    apply_parser = commands.add_parser("apply")
    apply_parser.add_argument("version")
    args = parser.parse_args(argv)
    if args.command == "check":
        check(args.metadata)
    else:
        apply(args.version)


if __name__ == "__main__":
    main(sys.argv[1:])
