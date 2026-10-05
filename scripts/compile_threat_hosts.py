#!/usr/bin/env python3
"""Validate and normalize Vola's pinned HaGeZi Threat Intelligence Feeds (mini) host asset."""

from __future__ import annotations

import argparse
from pathlib import Path

from compile_hagezi_hosts import CompiledHosts, compile_source


FORMAT_HEADER = "# Generated HaGeZi Threat Intelligence Feeds (mini) host rules. Do not edit by hand."
SOURCE_PATH = "adblock/tif.mini.txt"
LICENSE_ASSET = "hagezi_tif.LICENSE.txt"


def write_asset(output: Path, revision: str, compiled: CompiledHosts) -> None:
    lines = [
        FORMAT_HEADER,
        (
            "# Source: "
            "https://raw.githubusercontent.com/hagezi/dns-blocklists/"
            f"{revision}/{SOURCE_PATH}"
        ),
        f"# Source revision: {revision}",
        f"# Source SHA-256: {compiled.source_sha256}",
        f"# License: GPL-3.0; see {LICENSE_ASSET}",
        f"# Hosts: {len(compiled.hosts)}",
        *compiled.hosts,
    ]
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source-file", type=Path, required=True)
    parser.add_argument("--source-sha256", required=True)
    parser.add_argument("--revision", required=True)
    parser.add_argument("--output", type=Path, required=True)
    parser.add_argument("--min-hosts", type=int, default=150_000)
    parser.add_argument("--max-hosts", type=int, default=260_000)
    parser.add_argument("--max-output-bytes", type=int, default=4_800_000)
    args = parser.parse_args()

    # Threats are warned about even when an ad list already blocks the host, so nothing is excluded.
    compiled = compile_source(args.source_file.read_bytes(), args.source_sha256)
    if not args.min_hosts <= len(compiled.hosts) <= args.max_hosts:
        raise SystemExit(f"Unexpected threat host count: {len(compiled.hosts)}")
    write_asset(args.output, args.revision, compiled)
    if args.output.stat().st_size > args.max_output_bytes:
        args.output.unlink(missing_ok=True)
        raise SystemExit("Generated threat host asset exceeds byte budget")
    print(f"Wrote {len(compiled.hosts)} threat hosts")


if __name__ == "__main__":
    main()
