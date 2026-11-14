#!/usr/bin/env python3
"""Verify the installed Eyespie onboarding state across a real app force-stop.

This uses only Android's bundled UIAutomator and ADB, not a mock Compose tree.
The caller must install the debug APK, clear its state, and launch it first.
"""

import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

PACKAGE = "com.micrantha.eyespie.debug"
ACTIVITY = "com.micrantha.eyespie.android.MainActivity"
DUMP_PATH = "/sdcard/eyespie-ci-onboarding.xml"


def command(*args, check=True):
    return subprocess.run(
        args,
        text=True,
        capture_output=True,
        check=check,
        timeout=40,
    )


def find_label(root, label):
    for node in root.iter("node"):
        if node.get("text") == label or node.get("content-desc") == label:
            return node
    return None


def assert_visible(root, label):
    if find_label(root, label) is None:
        raise AssertionError(f"Expected UI label not visible: {label}")


def main():
    if len(sys.argv) != 1:
        raise SystemExit("usage: verify_installed_onboarding.py")

    serial = command("adb", "get-serialno").stdout.strip()
    if not re.fullmatch(r"emulator-[0-9]+", serial):
        raise RuntimeError(f"Refusing non-emulator Android target: {serial!r}")

    def adb(*args, check=True):
        return command("adb", "-s", serial, *args, check=check)

    def snapshot():
        # A failed dump must never be able to reuse a previous screen snapshot.
        adb("shell", "rm", "-f", DUMP_PATH)
        adb("shell", "uiautomator", "dump", DUMP_PATH)
        xml = adb("exec-out", "cat", DUMP_PATH).stdout
        return ET.fromstring(xml)

    def tap(node, label):
        bounds = re.fullmatch(r"\[(\d+),(\d+)\]\[(\d+),(\d+)\]", node.get("bounds", ""))
        if bounds is None:
            raise AssertionError(f"{label} control lacks usable accessibility bounds")
        left, top, right, bottom = map(int, bounds.groups())
        if right <= left or bottom <= top:
            raise AssertionError(f"{label} control has invalid bounds")
        adb("shell", "input", "tap", str((left + right) // 2), str((top + bottom) // 2))

    recovered_launcher_anr = False

    def wait_for(label):
        nonlocal recovered_launcher_anr
        deadline = time.monotonic() + 35
        last_error = None
        while time.monotonic() < deadline:
            try:
                root = snapshot()
                # An emulator boot-time launcher ANR can overlay the test app.
                # Recover *only* this exact system dialog, once per journey;
                # never dismiss a dialog from the app under test.
                if find_label(root, "Pixel Launcher isn't responding") is not None:
                    close = find_label(root, "Close app")
                    if recovered_launcher_anr or close is None:
                        raise AssertionError("Unrecoverable Pixel Launcher ANR obscures the app")
                    tap(close, "Pixel Launcher Close app")
                    recovered_launcher_anr = True
                    print("RECOVERED: dismissed emulator Pixel Launcher ANR", flush=True)
                    continue
                if find_label(root, label) is not None:
                    return root
            except (subprocess.CalledProcessError, ET.ParseError) as error:
                last_error = error
            time.sleep(1)
        raise AssertionError(f"Timed out waiting for {label!r}; last UI error: {last_error}")

    wait_for("Play locally")
    root = wait_for("Skip")
    # Refresh the existing CI screenshot only after the onboarding UI is
    # observable; an earlier capture may show a transient emulator dialog.
    image = subprocess.run(
        ("adb", "-s", serial, "exec-out", "screencap", "-p"),
        check=True,
        capture_output=True,
        timeout=40,
    ).stdout
    if not image.startswith(b"\x89PNG\r\n\x1a\n"):
        raise AssertionError("Installed onboarding screenshot is not a PNG")
    with open("build/ci/android-screen-instrumentation/installed-onboarding.png", "wb") as capture:
        capture.write(image)
    tap(find_label(root, "Skip"), "Skip")

    wait_for("Field desk")
    wait_for("Local Mode")
    print("PASS: installed app dismisses onboarding and displays local home", flush=True)

    adb("shell", "am", "force-stop", PACKAGE)
    if adb("shell", "pidof", PACKAGE, check=False).stdout.strip():
        raise AssertionError("App remained running after force-stop")
    adb("shell", "am", "start", "-W", "-n", f"{PACKAGE}/{ACTIVITY}")

    wait_for("Field desk")
    root = wait_for("Local Mode")
    assert_visible(root, "Field desk")
    if find_label(root, "Play locally") is not None:
        raise AssertionError("Onboarding unexpectedly returned after process restart")
    print("PASS: fresh app process restores home without onboarding", flush=True)

    # Confirm the assertion oracle rejects a *wrong* expectation.
    try:
        assert_visible(root, "Play locally")
    except AssertionError:
        print("PASS: deliberately false onboarding assertion was rejected", flush=True)
    else:
        raise AssertionError("Negative control did not reject the false expectation")


if __name__ == "__main__":
    main()
