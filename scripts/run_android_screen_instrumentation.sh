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

# Exercise the interaction suite with enlarged system text. The app process is launched
# after this setting is applied, so Compose reads the updated font scale.
adb shell settings put system font_scale 1.30
adb shell settings get system font_scale | tee "$ARTIFACT_DIR/font-scale.txt"

# Invoke the Gradle gate directly here. The mise task is useful for local ergonomics, but
# this CI script must preserve the Gradle command's exit code exactly before collecting
# installed-build evidence.
set +e
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.package=com.micrantha.eyespie.features \
  --no-daemon --stacktrace
status=$?
set -e

# Keep one representative screenshot from the actual installed application alongside
# pure Compose interaction evidence. Clear app data so the launch is deterministic and
# lands on onboarding, then restore normal font scale for the canonical visual review.
adb shell settings put system font_scale 1.0
adb shell pm clear com.micrantha.eyespie > "$ARTIFACT_DIR/pm-clear.txt" || true
adb shell monkey -p com.micrantha.eyespie -c android.intent.category.LAUNCHER 1 \
  > "$ARTIFACT_DIR/installed-launch.txt" 2>&1 || true
sleep 3
adb exec-out screencap -p > "$ARTIFACT_DIR/installed-onboarding.png" || true
adb shell dumpsys window windows > "$ARTIFACT_DIR/installed-window.txt" 2>&1 || true

adb logcat -d > "$ARTIFACT_DIR/logcat.txt" 2>&1 || true
exit "$status"
