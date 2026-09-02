#!/usr/bin/env python3
"""Validate Eyespie brand sources and platform wiring against the canonical palette."""

from __future__ import annotations

import hashlib
import json
import plistlib
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def fail(message: str) -> None:
    raise AssertionError(message)


def read_text(path: str) -> str:
    return (ROOT / path).read_text(encoding="utf-8")


def load_json(path: str) -> dict:
    return json.loads(read_text(path))


def sha256(path: str) -> str:
    return hashlib.sha256((ROOT / path).read_bytes()).hexdigest()


def xml_colors(path: str) -> dict[str, str]:
    root = ET.parse(ROOT / path).getroot()
    return {
        element.attrib["name"]: (element.text or "").strip()
        for element in root.findall("color")
    }


def asset_color(path: str) -> str:
    data = load_json(path)
    colors = data.get("colors", [])
    if len(colors) != 1:
        fail(f"{path}: expected exactly one universal color")
    color = colors[0].get("color", {})
    if color.get("color-space") != "srgb":
        fail(f"{path}: expected sRGB color space")
    components = color.get("components", {})
    channels = []
    for name in ("red", "green", "blue"):
        value = components.get(name)
        if value is None:
            fail(f"{path}: missing {name} component")
        channels.append(round(float(value) * 255))
    return "#" + "".join(f"{value:02X}" for value in channels)


def main() -> int:
    palette_data = load_json("docs/design/brand/palette.json")
    palette = palette_data["colors"]

    expected = {
        "field": "#D9E3DF",
        "petal": "#829FC0",
        "petal-inner": "#6F8CA8",
        "throat": "#EEE7CD",
        "iris": "#B59C69",
        "pupil": "#263947",
        "ink": "#314956",
        "white": "#F5F5F0",
    }
    if palette != expected:
        fail("palette.json does not match the reviewed Micrantha Lens palette")

    android = xml_colors("eyespie/src/androidMain/res/values/brand_colors.xml")
    for name, value in expected.items():
        resource_name = "brand_" + name.replace("-", "_")
        if android.get(resource_name) != value:
            fail(f"Android {resource_name} drifted from palette.json")

    icon_colors = xml_colors("eyespie/src/androidMain/res/values/icon_colors.xml")
    if icon_colors.get("ic_launcher_background") != "@color/brand_field":
        fail("Android launcher background must alias the canonical brand field")

    base_styles = read_text("eyespie/src/androidMain/res/values/styles.xml")
    if '<item name="android:windowBackground">@color/brand_field</item>' not in base_styles:
        fail("Android base starting window must use brand_field")

    v31_styles = read_text("eyespie/src/androidMain/res/values-v31/styles.xml")
    required_v31 = (
        '<item name="android:windowSplashScreenBackground">@color/brand_field</item>',
        '<item name="android:windowSplashScreenAnimatedIcon">@drawable/ic_launcher_foreground</item>',
    )
    for fragment in required_v31:
        if fragment not in v31_styles:
            fail(f"Android 12+ splash contract missing: {fragment}")

    theme = read_text("eyespie/src/commonMain/kotlin/com/micrantha/eyespie/presentation/theme/EyespieColors.kt")
    for value in expected.values():
        kotlin_literal = "0xFF" + value.removeprefix("#")
        if kotlin_literal not in theme:
            fail(f"Compose theme is missing canonical color {value}")

    if asset_color("iosApp/iosApp/Assets.xcassets/BrandLaunchBackground.colorset/Contents.json") != expected["field"]:
        fail("iOS launch background drifted from brand field")
    if asset_color("iosApp/iosApp/Assets.xcassets/AccentColor.colorset/Contents.json") != expected["petal-inner"]:
        fail("iOS accent color drifted from petal-inner")

    with (ROOT / "iosApp/iosApp/Info.plist").open("rb") as stream:
        info = plistlib.load(stream)
    launch = info.get("UILaunchScreen")
    if not isinstance(launch, dict) or launch.get("UIColorName") != "BrandLaunchBackground":
        fail("iOS UILaunchScreen must use BrandLaunchBackground")
    if "UIImageName" in launch:
        fail("iOS launch is intentionally background-only; do not introduce a second launch mark")

    master = "docs/design/brand/eyespie-app-icon-1024.png"
    ios_icon = "iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/eyespie-app-icon-1024.png"
    if sha256(master) != sha256(ios_icon):
        fail("iOS app icon must remain byte-identical to the canonical 1024px master")

    app_icon_catalog = load_json("iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/Contents.json")
    filenames = {item.get("filename") for item in app_icon_catalog.get("images", [])}
    if "eyespie-app-icon-1024.png" not in filenames:
        fail("iOS AppIcon catalog does not reference the canonical master")

    print("Eyespie brand assets: OK")
    return 0


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except (AssertionError, KeyError, OSError, ValueError, ET.ParseError) as error:
        print(f"Eyespie brand assets: FAILED: {error}", file=sys.stderr)
        raise SystemExit(1)
