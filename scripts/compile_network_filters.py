#!/usr/bin/env python3
"""Compile EasyList/EasyPrivacy network filters for Vola's own Gecko blocker.

The host compiler (compile_easylist_hosts.py) keeps the plain `||host^` rules. This one keeps the
rest that a Gecko WebExtension can apply exactly: URL patterns with `||`, `|`, `^` and `*`,
resource types (Gecko's webRequest reports them), party and `domain=` options, and `@@`
exceptions. Each rule carries one URL token, so the blocker only tries rules whose token occurs in
the request URL (the way uBlock Origin indexes its filters).
"""

from __future__ import annotations

import argparse
import base64
import re
from dataclasses import dataclass
from pathlib import Path

from compile_easylist_cosmetic import template_sources
from compile_easylist_hosts import CompileStats as HostStats
from compile_easylist_hosts import active_lines, anchored_host, badfilter_targets, canonical_filter


FORMAT_HEADER = "candy-network-filter:1"
# ABP resource types -> the Gecko webRequest types they cover. `document` stays out on purpose:
# Vola never blocks a page itself.
RESOURCE_TYPES = {
    "script": ("script",),
    "image": ("image", "imageset"),
    "stylesheet": ("stylesheet",),
    "object": ("object", "object_subrequest"),
    "object-subrequest": ("object_subrequest",),
    "xmlhttprequest": ("xmlhttprequest",),
    "xhr": ("xmlhttprequest",),
    "subdocument": ("sub_frame",),
    "frame": ("sub_frame",),
    "media": ("media",),
    "font": ("font",),
    "ping": ("ping", "beacon"),
    "websocket": ("websocket",),
    "other": ("other", "xml_dtd", "xslt", "csp_report", "web_manifest", "speculative"),
}
ALL_TYPES = tuple(sorted({gecko for values in RESOURCE_TYPES.values() for gecko in values}))
PARTY_OPTIONS = {"third-party": "3", "3p": "3", "~third-party": "1", "first-party": "1", "1p": "1"}
SAFE_LABEL = re.compile(r"^[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?$")
TOKEN_CHARACTERS = frozenset("abcdefghijklmnopqrstuvwxyz0123456789%")
# Tokens on nearly every URL would put their rules in front of every request.
COMMON_TOKENS = frozenset({
    "http", "https", "www", "com", "net", "org", "js", "css", "html", "php", "png", "jpg", "gif",
    "svg", "json", "api", "static", "cdn", "assets", "img", "images", "min",
})
MAX_PATTERN_LENGTH = 512
MAX_WILDCARDS = 8


@dataclass(frozen=True, order=True)
class NetworkRecord:
    action: str
    token: str
    pattern: str
    types: tuple[str, ...]
    page_hosts: tuple[str, ...]
    excluded_page_hosts: tuple[str, ...]
    party: str


@dataclass
class CompileStats:
    source_lines: int = 0
    block_rules: int = 0
    allow_rules: int = 0
    tokenless_rules: int = 0
    host_list_rules: int = 0
    unsupported_rules: int = 0
    conditional_lines: int = 0
    badfilter_rules: int = 0


def canonical_page_host(raw: str) -> str | None:
    value = raw.strip().lower().rstrip(".")
    wildcard = value.endswith(".*")
    fixed = value[:-2] if wildcard else value
    if "*" in fixed or not fixed:
        return None
    try:
        ascii_value = fixed.encode("idna").decode("ascii")
    except UnicodeError:
        return None
    labels = ascii_value.split(".")
    if len(labels) < (1 if wildcard else 2) or any(not SAFE_LABEL.fullmatch(label) for label in labels):
        return None
    return f"{ascii_value}.*" if wildcard else ascii_value


def parse_domains(value: str) -> tuple[tuple[str, ...], tuple[str, ...]] | None:
    positives: set[str] = set()
    negatives: set[str] = set()
    for raw in value.split("|"):
        excluded = raw.startswith("~")
        host = canonical_page_host(raw[1:] if excluded else raw)
        if host is None:
            return None
        (negatives if excluded else positives).add(host)
    return tuple(sorted(positives)), tuple(sorted(negatives))


def best_token(pattern: str) -> str:
    """The longest URL token the pattern guarantees, or "" when it guarantees none.

    A run of token characters counts only when both ends are fixed: a literal separator, an
    anchor or `^` on each side. Next to `*`, or at an unanchored edge, the URL may continue the run.
    """
    body = pattern
    start_bounded = False
    if body.startswith("||"):
        body = body[2:]
        start_bounded = True
    elif body.startswith("|"):
        body = body[1:]
        start_bounded = True
    end_bounded = False
    if body.endswith("|"):
        body = body[:-1]
        end_bounded = True
    best = ""
    index = 0
    while index < len(body):
        if body[index] not in TOKEN_CHARACTERS:
            index += 1
            continue
        start = index
        while index < len(body) and body[index] in TOKEN_CHARACTERS:
            index += 1
        before = body[start - 1] if start > 0 else None
        after = body[index] if index < len(body) else None
        left_fixed = (before is None and start_bounded) or (before is not None and before != "*")
        right_fixed = (after is None and end_bounded) or (after is not None and after != "*")
        token = body[start:index]
        if left_fixed and right_fixed and len(token) >= 3 and token not in COMMON_TOKENS:
            if len(token) > len(best):
                best = token
    return best


def parse_rule(value: str, disabled: set[str], stats: CompileStats) -> NetworkRecord | None:
    if canonical_filter(value) in disabled:
        stats.badfilter_rules += 1
        return None
    action = "A" if value.startswith("@@") else "B"
    body = value.removeprefix("@@")
    pattern, separator, raw_options = body.rpartition("$")
    if not separator or "/" in raw_options:
        # No options, or a `$` inside the URL pattern itself.
        pattern, raw_options = body, ""
    options = [option for option in raw_options.split(",") if option]
    if "badfilter" in options:
        stats.badfilter_rules += 1
        return None
    if action == "B" and anchored_host(pattern) and set(options) <= {"third-party", "3p"}:
        stats.host_list_rules += 1
        return None

    party = "*"
    included_types: set[str] = set()
    excluded_types: set[str] = set()
    domain_value: str | None = None
    for option in options:
        option = option.lower()
        if option in PARTY_OPTIONS:
            if party not in ("*", PARTY_OPTIONS[option]):
                return None
            party = PARTY_OPTIONS[option]
        elif option.startswith(("domain=", "from=")):
            if domain_value is not None:
                return None
            domain_value = option.split("=", 1)[1]
        elif option in RESOURCE_TYPES:
            included_types.update(RESOURCE_TYPES[option])
        elif option.startswith("~") and option[1:] in RESOURCE_TYPES:
            excluded_types.update(RESOURCE_TYPES[option[1:]])
        else:
            # match-case, important, redirect, csp, removeparam, document, popup, generichide and
            # the rest change what a match does, not only whether it matches: leave them to WebView
            # or to later steps rather than apply them half way.
            return None
    if included_types and excluded_types:
        return None
    if excluded_types:
        types = tuple(sorted(set(ALL_TYPES) - excluded_types))
    else:
        types = tuple(sorted(included_types))
    pages: tuple[str, ...] = ()
    excluded_pages: tuple[str, ...] = ()
    if domain_value is not None:
        parsed = parse_domains(domain_value)
        if parsed is None:
            return None
        pages, excluded_pages = parsed

    pattern = pattern.lower()
    if (
        not pattern
        or pattern in ("*", "|", "||")
        or (pattern.startswith("/") and pattern.endswith("/") and len(pattern) > 1)
        or len(pattern) > MAX_PATTERN_LENGTH
        or pattern.count("*") > MAX_WILDCARDS
        or "\\" in pattern
        or any(ord(character) < 33 or ord(character) > 126 for character in pattern)
    ):
        return None
    token = best_token(pattern)
    if not token:
        # A rule without a token runs on every request: keep only narrow ones.
        if not pages:
            return None
        stats.tokenless_rules += 1
    return NetworkRecord(action, token or "-", pattern, types, pages, excluded_pages, party)


def compile_sources(source_paths: list[Path]) -> tuple[list[NetworkRecord], CompileStats]:
    host_stats = HostStats()
    lines = active_lines(source_paths, host_stats)
    stats = CompileStats(conditional_lines=host_stats.conditional_lines)
    disabled = badfilter_targets(lines)
    records: set[NetworkRecord] = set()
    for value in lines:
        if not value or value.startswith(("!", "[")):
            continue
        if any(marker in value for marker in ("##", "#@#", "#?#", "#$#", "#%#")):
            continue
        stats.source_lines += 1
        record = parse_rule(value, disabled, stats)
        if record is None:
            continue
        records.add(record)
    ordered = sorted(records)
    stats.block_rules = sum(record.action == "B" for record in ordered)
    stats.allow_rules = sum(record.action == "A" for record in ordered)
    stats.unsupported_rules = (
        stats.source_lines - len(ordered) - stats.host_list_rules - stats.badfilter_rules
    )
    return ordered, stats


def encode(value: str) -> str:
    return base64.urlsafe_b64encode(value.encode("utf-8")).decode("ascii").rstrip("=")


def write_asset(output: Path, revision: str, records: list[NetworkRecord], stats: CompileStats) -> None:
    lines = [
        FORMAT_HEADER,
        "# Generated EasyList/EasyPrivacy network filters for Vola's Gecko blocker. Do not edit by hand.",
        "# Format: action<TAB>token|-<TAB>base64url(pattern)<TAB>types|*<TAB>pages|-<TAB>excluded|-<TAB>party",
        "# Source: https://github.com/easylist/easylist",
        f"# Source revision: {revision}",
        "# Source graph: easylist.template + easyprivacy.template",
        "# License: CC BY-SA 3.0 or later; see content_filter.LICENSE.txt",
        f"# Rules: {len(records)}",
        f"# Block rules: {stats.block_rules}",
        f"# Allow rules: {stats.allow_rules}",
        f"# Tokenless rules: {stats.tokenless_rules}",
        f"# Left to the host list: {stats.host_list_rules}",
        f"# Skipped unsupported rules: {stats.unsupported_rules}",
        f"# Skipped conditional lines: {stats.conditional_lines}",
    ]
    for record in records:
        lines.append("\t".join((
            record.action,
            record.token,
            encode(record.pattern),
            ",".join(record.types) if record.types else "*",
            ",".join(record.page_hosts) or "-",
            ",".join(record.excluded_page_hosts) or "-",
            record.party,
        )))
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-root", type=Path, required=True)
    parser.add_argument("--template", action="append", required=True)
    parser.add_argument("--revision", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--min-rules", type=int, default=5_000)
    parser.add_argument("--max-bytes", type=int, default=1_500_000)
    args = parser.parse_args()

    records, stats = compile_sources(template_sources(args.source_root, args.template))
    if len(records) < args.min_rules:
        raise SystemExit(f"Unexpected network filter count: {len(records)}")
    write_asset(args.output, args.revision, records, stats)
    if args.output.stat().st_size > args.max_bytes:
        args.output.unlink(missing_ok=True)
        raise SystemExit("Generated network filter asset exceeds byte budget")
    print(f"Wrote {len(records)} network filters ({stats.block_rules} block, {stats.allow_rules} allow)")


if __name__ == "__main__":
    main()
