#!/usr/bin/env bash
set -euo pipefail

ARTIFACT_DIR="build/ci/android-screen-instrumentation"
PACKAGE_ID="com.micrantha.eyespie.debug"
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

# connectedDebugAndroidTest removes the tested application when it completes. Reinstall
# the exact debug APK produced by that same build before collecting installed-app evidence.
adb shell settings put system font_scale 1.0
apk_path="$(find eyespie/build/outputs/apk/debug -maxdepth 1 -type f -name '*.apk' | head -n 1)"
if [[ -z "$apk_path" ]]; then
  echo "Debug APK missing after instrumentation build" >&2
  exit 1
fi
adb install -r "$apk_path" | tee "$ARTIFACT_DIR/installed-apk.txt"
adb shell pm clear "$PACKAGE_ID" | tee "$ARTIFACT_DIR/pm-clear.txt"
adb shell monkey -p "$PACKAGE_ID" -c android.intent.category.LAUNCHER 1 \
  > "$ARTIFACT_DIR/installed-launch.txt" 2>&1
sleep 3
adb shell dumpsys window windows > "$ARTIFACT_DIR/installed-window.txt" 2>&1
if ! grep -q "$PACKAGE_ID" "$ARTIFACT_DIR/installed-window.txt"; then
  echo "Installed Eyespie debug app is not the active window" >&2
  exit 1
fi
adb exec-out screencap -p > "$ARTIFACT_DIR/installed-onboarding.png"
if [[ ! -s "$ARTIFACT_DIR/installed-onboarding.png" ]]; then
  echo "Installed-build screenshot was not captured" >&2
  exit 1
fi

adb logcat -d > "$ARTIFACT_DIR/logcat.txt" 2>&1 || true
exit "$status"
