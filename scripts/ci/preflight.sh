#!/bin/bash
# What CI checks first, before a push (docs/vola/claude-tooling.md): the quality gates, the script
# tests and, when the Android SDK is installed, the Kotlin compile and the unit tests.
#
# Usage: scripts/ci/preflight.sh [--fast]   --fast (or VOLA_PREFLIGHT=fast) skips Gradle.
# .githooks/pre-push runs it; `git push --no-verify` skips it when there is a reason to.
set -uo pipefail
cd "$(git rev-parse --show-toplevel)"

fast=false
[ "${1:-}" = "--fast" ] || [ "${VOLA_PREFLIGHT:-}" = "fast" ] && fast=true
failed=()
log="$(mktemp)"
trap 'rm -f "$log"' EXIT
# Runs one check quietly; its output shows only when it fails.
step() {
  local name="$1"
  shift
  if "$@" >"$log" 2>&1; then
    echo "✓ $name"
  else
    echo "✗ $name"
    tail -n 60 "$log"
    failed+=("$name")
  fi
}

git fetch --quiet origin main 2>/dev/null || echo "(no fetch: tokens compare with the local origin/main)"
# Lines this branch adds, as CI sees them in the pull request: from where it left main.
base="$(git merge-base HEAD origin/main)"

# Quality gates: every gate the script offers, as in build.yml.
gates="$(python3 scripts/ci/quality_gates.py -h | grep -o '{[^}]*}' | head -n 1 | tr -d '{}' | tr ',' ' ')"
for gate in $gates; do
  if [ "$gate" = tokens ]; then
    step "gate tokens" python3 scripts/ci/quality_gates.py tokens --base "$base"
  else
    step "gate $gate" python3 scripts/ci/quality_gates.py "$gate"
  fi
done

# Script tests: the list build.yml runs (the APK checks need built APKs and stay in CI).
for test in scripts/test_compile_*.py scripts/test_generate_gecko_default_extensions.py \
  scripts/test_translations.py scripts/test_geckoview_latest.py \
  scripts/test_quality_gates.py scripts/test_a11y_audit.py scripts/ci/test_*.py; do
  [ -f "$test" ] && step "$test" python3 "$test"
done
step "default extensions" python3 scripts/generate_gecko_default_extensions.py verify
if command -v node >/dev/null; then
  step "node tests" node --test scripts/*.test.mjs
fi

sdk="${ANDROID_HOME:-$HOME/android-sdk}"
if [ "$fast" = true ]; then
  echo "(--fast: no Gradle)"
elif [ ! -f "$sdk/.vola-sdk-ready" ]; then
  echo "(no Android SDK yet: the session start hook is still installing it, see $sdk/.vola-sdk-install.log)"
else
  export ANDROID_HOME="$sdk" ANDROID_SDK_ROOT="$sdk"
  step "Kotlin compile and unit tests" ./gradlew --console=plain --quiet \
    compileFullDebugKotlin testFullDebugUnitTest
fi

if [ "${#failed[@]}" -gt 0 ]; then
  echo "✗ preflight failed: ${failed[*]}"
  exit 1
fi
echo "✓ preflight passed"
