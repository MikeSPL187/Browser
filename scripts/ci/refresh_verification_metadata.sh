#!/bin/bash
# Records the sha256 of every artifact Gradle resolves in gradle/verification-metadata.xml (N-05), so a
# build fails on a dependency whose checksum differs from the one recorded. Run it on a branch that
# changes a dependency (Dependabot, the GeckoView update) and commit the file with that change.
#
# Usage: scripts/ci/refresh_verification_metadata.sh [--quick]
#   --quick resolves the dependency graphs only: enough when a library's version changes (GeckoView);
#   without it the build, tests, lint, release and benchmarks run too, which also fetch their tools.
#
# With a warm Gradle cache, parent POMs and Gradle module files of cached dependencies are not fetched
# again and so not recorded, while CI starts empty and needs them (#498). Run it in CI, or locally
# with an empty GRADLE_USER_HOME.
set -euo pipefail
cd "$(git rev-parse --show-toplevel)"

tasks=(resolveAllConfigurations)
if [ "${1:-}" != "--quick" ]; then
  tasks+=(
    lintFullDebug lintSystemwebviewDebug testFullDebugUnitTest :shared:testDebugUnitTest
    assembleFullDebugAndroidTest assembleSystemwebviewDebug
    assembleFullLocalRelease assembleSystemwebviewLocalRelease :baselineprofile:assemble
  )
fi

./gradlew --console=plain --no-configuration-cache \
  -I gradle/resolve-all.init.gradle \
  --write-verification-metadata sha256 \
  "${tasks[@]}"
git --no-pager diff --stat -- gradle/verification-metadata.xml
