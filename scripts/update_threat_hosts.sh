#!/bin/sh
set -eu

SOURCE_REVISION="b3f10c70552d10259d966e39402ac73ec468d47f"
SOURCE_SHA256="96c56bf35e00df06a6198a7f62abe2d6f357dfa64a5041846c1303099b22496d"
LICENSE_SHA256="3972dc9744f6499f0f9b2dbf76696f2ae7ad8af9b23dde66d6af86c9dfb36986"
SOURCE_URL="https://raw.githubusercontent.com/hagezi/dns-blocklists/$SOURCE_REVISION/adblock/tif.mini.txt"
LICENSE_URL="https://raw.githubusercontent.com/hagezi/dns-blocklists/$SOURCE_REVISION/LICENSE"
PROJECT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
ASSET_DIR="$PROJECT_DIR/app/src/main/assets"
TEMP_DIR=$(mktemp -d)
trap 'rm -rf "$TEMP_DIR"' EXIT HUP INT TERM

# Phishing, malware and scam hosts for the «Dangerous site» warning. The list is checked on the
# phone only. To refresh it, pin a newer revision and its SHA-256 here, then run this script.

curl --fail --location --silent --show-error "$SOURCE_URL" > "$TEMP_DIR/source.txt"
curl --fail --location --silent --show-error "$LICENSE_URL" > "$TEMP_DIR/license.txt"
ACTUAL_LICENSE_SHA256=$(sha256sum "$TEMP_DIR/license.txt" | cut -d ' ' -f 1)
if [ "$ACTUAL_LICENSE_SHA256" != "$LICENSE_SHA256" ]; then
    echo "HaGeZi license SHA-256 mismatch: $ACTUAL_LICENSE_SHA256" >&2
    exit 1
fi

python3 "$PROJECT_DIR/scripts/compile_threat_hosts.py" \
    --source-file "$TEMP_DIR/source.txt" \
    --source-sha256 "$SOURCE_SHA256" \
    --revision "$SOURCE_REVISION" \
    --output "$TEMP_DIR/threat_hosts.txt"

{
    printf '%s\n' 'HaGeZi Threat Intelligence Feeds (mini) attribution and license'
    printf '%s\n' '==============================================================='
    printf '\n'
    printf 'Source: %s\n' "$SOURCE_URL"
    printf 'Source revision: %s\n' "$SOURCE_REVISION"
    printf 'Source SHA-256: %s\n' "$SOURCE_SHA256"
    printf 'License SHA-256: %s\n' "$LICENSE_SHA256"
    printf '%s\n' 'Upstream project: https://github.com/hagezi/dns-blocklists'
    printf '%s\n' 'License: GNU General Public License version 3'
    printf '\n'
    printf '%s\n' 'Modification notice:'
    printf '%s\n' '- Exact host rules are validated, normalized, sorted, and deduplicated.'
    printf '%s\n' '- scripts/update_threat_hosts.sh and scripts/compile_threat_hosts.py are the transformation source.'
    printf '\n'
    cat "$TEMP_DIR/license.txt"
} > "$TEMP_DIR/hagezi_tif.LICENSE.txt"

chmod 644 "$TEMP_DIR/threat_hosts.txt" "$TEMP_DIR/hagezi_tif.LICENSE.txt"
mv "$TEMP_DIR/threat_hosts.txt" "$ASSET_DIR/threat_hosts.txt"
mv "$TEMP_DIR/hagezi_tif.LICENSE.txt" "$ASSET_DIR/hagezi_tif.LICENSE.txt"
