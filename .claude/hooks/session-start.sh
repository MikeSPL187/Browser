#!/bin/bash
# Prepares a Claude Code cloud session for Vola (docs/vola/claude-tooling.md): the repository's git
# hooks, then the Android SDK in the background, so Gradle can compile and run unit tests.
set -euo pipefail

[ "${CLAUDE_CODE_REMOTE:-}" = "true" ] || exit 0
cd "${CLAUDE_PROJECT_DIR:-$(pwd)}"

# .githooks/pre-push runs scripts/ci/preflight.sh before every push.
git config core.hooksPath .githooks

# Maven Central answers 429 to bursts from the cloud's shared address: Gradle retries longer.
mkdir -p "$HOME/.gradle"
grep -q "repository.max.retries" "$HOME/.gradle/gradle.properties" 2>/dev/null || cat >>"$HOME/.gradle/gradle.properties" <<'PROPS'
systemProp.org.gradle.internal.repository.max.retries=12
systemProp.org.gradle.internal.repository.initial.backoff=3000
PROPS

SDK="${ANDROID_HOME:-$HOME/android-sdk}"
PLATFORM="platforms;android-37.1" # build.yml: ANDROID_PLATFORM
READY="$SDK/.vola-sdk-ready"
if [ -n "${CLAUDE_ENV_FILE:-}" ]; then
  {
    echo "export ANDROID_HOME=\"$SDK\""
    echo "export ANDROID_SDK_ROOT=\"$SDK\""
  } >> "$CLAUDE_ENV_FILE"
fi
[ -f "$READY" ] && exit 0

# The rest takes a few minutes; the session starts meanwhile. preflight.sh waits for $READY.
echo '{"async": true, "asyncTimeout": 1200000}'
LOG="$SDK/.vola-sdk-install.log"
mkdir -p "$SDK"
exec >"$LOG" 2>&1

if [ ! -x "$SDK/cmdline-tools/latest/bin/sdkmanager" ]; then
  zip_name="$(curl -fsSL https://dl.google.com/android/repository/repository2-3.xml |
    grep -o 'commandlinetools-linux-[0-9]*_latest\.zip' | sort -V | tail -n 1)"
  tmp="$(mktemp -d)"
  curl -fsSL "https://dl.google.com/android/repository/$zip_name" -o "$tmp/tools.zip"
  unzip -q "$tmp/tools.zip" -d "$tmp"
  mkdir -p "$SDK/cmdline-tools"
  rm -rf "$SDK/cmdline-tools/latest"
  mv "$tmp/cmdline-tools" "$SDK/cmdline-tools/latest"
  rm -rf "$tmp"
fi
sdkmanager="$SDK/cmdline-tools/latest/bin/sdkmanager"
# Licences accepted here let Gradle fetch the build tools the Android plugin asks for.
yes | "$sdkmanager" --sdk_root="$SDK" --licenses >/dev/null || true
"$sdkmanager" --sdk_root="$SDK" "platform-tools" "$PLATFORM"
touch "$READY"
echo "Android SDK ready in $SDK"
