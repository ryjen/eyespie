#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_DIR="build/ci/android-screen-instrumentation"
mkdir -p "$ARTIFACT_DIR"

adb wait-for-device
adb devices -l | tee "$ARTIFACT_DIR/adb-devices.txt"

boot_completed="$(adb shell getprop sys.boot_completed)"
printf '%s\n' "$boot_completed" | tee "$ARTIFACT_DIR/sys-boot-completed.txt"
if [[ "$boot_completed" != "1" ]]; then
  echo "Android emulator did not report completed boot" >&2
  exit 1
fi

adb shell getprop | sort > "$ARTIFACT_DIR/emulator-getprop.txt"

set +e
mise run screen-test
status=$?
set -e

adb logcat -d > "$ARTIFACT_DIR/logcat.txt" 2>&1 || true
exit "$status"
