#!/usr/bin/env python3
"""Verify and render non-sensitive Eyespie closed-alpha candidate identity."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
import tomllib
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
VERSION_CONFIG = ROOT / "iosApp/Configuration/Version.xcconfig"
MEDIAPIPE_CONFIG = ROOT / "iosApp/Configuration/MediaPipe.xcconfig"
BUILD_GRADLE = ROOT / "eyespie/build.gradle.kts"
INFO_PLIST = ROOT / "iosApp/iosApp/Info.plist"
XCODE_PROJECT = ROOT / "iosApp/iosApp.xcodeproj/project.pbxproj"
DEBUG_XCCONFIG = ROOT / "iosApp/Configuration/Config.debug.xcconfig"
RELEASE_XCCONFIG = ROOT / "iosApp/Configuration/Config.release.xcconfig"
VERSIONS_TOML = ROOT / "gradle/libs.versions.toml"
CORE_SOURCE = ROOT / "eyespie/src/commonMain/kotlin/com/micrantha/eyespie/core/Core.kt"
EMBEDDING_SOURCE = ROOT / "eyespie/src/commonMain/kotlin/com/micrantha/eyespie/imaging/ImageEmbedding.kt"
BUNDLE_SOURCE = ROOT / "eyespie/src/commonMain/kotlin/com/micrantha/eyespie/sharing/GameBundle.kt"
SQLDELIGHT_DIR = ROOT / "eyespie/src/commonMain/sqldelight/com/micrantha/eyespie/data"

CANDIDATE_IDENTITY_SCHEMA_VERSION = 2
EXPECTED_IOS_BUNDLE_ID = "com.micrantha.eyespie"
SEMVER_RE = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.-]+)?$")
NUMERIC_VERSION_RE = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+(?:\.[0-9]+)?$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


class CandidateIdentityError(ValueError):
    pass


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        raise CandidateIdentityError(f"cannot read required file: {path.relative_to(ROOT)}") from exc


def simple_xcconfig_values(path: Path) -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in read_text(path).splitlines():
        line = raw_line.strip()
        if not line or line.startswith("//"):
            continue
        if "=" not in line:
            raise CandidateIdentityError(
                f"{path.name} contains a non-assignment line"
            )
        key, value = (part.strip() for part in line.split("=", 1))
        if not key or not value:
            raise CandidateIdentityError(f"{path.name} contains an empty key/value")
        if key in values:
            raise CandidateIdentityError(f"duplicate {path.name} key: {key}")
        values[key] = value
    return values


def version_config_values() -> dict[str, str]:
    values = simple_xcconfig_values(VERSION_CONFIG)
    if set(values) != {"APP_VERSION", "APP_BUILD"}:
        raise CandidateIdentityError("Version.xcconfig must define exactly APP_VERSION and APP_BUILD")
    if not SEMVER_RE.fullmatch(values["APP_VERSION"]):
        raise CandidateIdentityError("APP_VERSION must be semantic-version shaped")
    try:
        build = int(values["APP_BUILD"])
    except ValueError as exc:
        raise CandidateIdentityError("APP_BUILD must be an integer") from exc
    if build <= 0 or str(build) != values["APP_BUILD"]:
        raise CandidateIdentityError("APP_BUILD must be a canonical positive integer")
    return values


def ios_mediapipe_version() -> str:
    values = simple_xcconfig_values(MEDIAPIPE_CONFIG)
    if set(values) != {"IOS_MEDIAPIPE_TASKS_VERSION"}:
        raise CandidateIdentityError(
            "MediaPipe.xcconfig must define exactly IOS_MEDIAPIPE_TASKS_VERSION"
        )
    version = values["IOS_MEDIAPIPE_TASKS_VERSION"]
    if not NUMERIC_VERSION_RE.fullmatch(version):
        raise CandidateIdentityError(
            "IOS_MEDIAPIPE_TASKS_VERSION must be numeric-version shaped"
        )
    return version


def require_regex(text: str, pattern: str, description: str) -> re.Match[str]:
    match = re.search(pattern, text, flags=re.MULTILINE | re.DOTALL)
    if match is None:
        raise CandidateIdentityError(f"cannot derive {description}")
    return match


def kotlin_int(text: str, name: str) -> int:
    match = require_regex(text, rf"const val {re.escape(name)}\s*=\s*([0-9]+)", name)
    return int(match.group(1))


def kotlin_float(text: str, name: str) -> float:
    match = require_regex(
        text,
        rf"const val {re.escape(name)}(?:\s*:\s*(?:Double|Float))?\s*=\s*(-?[0-9]+(?:\.[0-9]+)?)",
        name,
    )
    value = float(match.group(1))
    if not -1.0 <= value <= 1.0:
        raise CandidateIdentityError(f"{name} must be within [-1, 1]")
    return value


def kotlin_string(text: str, name: str) -> str:
    match = require_regex(text, rf'const val {re.escape(name)}\s*=\s*"([^"]+)"', name)
    return match.group(1)


def git(*args: str) -> str:
    try:
        return subprocess.check_output(
            ["git", "-C", str(ROOT), *args],
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
    except (OSError, subprocess.CalledProcessError) as exc:
        raise CandidateIdentityError("git metadata is unavailable") from exc


def current_sqldelight_schema_version() -> int:
    migrations: list[int] = []
    for path in SQLDELIGHT_DIR.glob("*.sqm"):
        if path.stem.isdigit():
            migrations.append(int(path.stem))
    return max(migrations, default=0) + 1


def verify_wiring(version: str, build: int, ios_mediapipe: str) -> None:
    gradle = read_text(BUILD_GRADLE)
    info = read_text(INFO_PLIST)
    xcode_project = read_text(XCODE_PROJECT)
    debug_config = read_text(DEBUG_XCCONFIG)
    release_config = read_text(RELEASE_XCCONFIG)

    required_gradle_fragments = (
        'xcconfigValue("iosApp/Configuration/Version.xcconfig", "APP_VERSION")',
        'xcconfigValue("iosApp/Configuration/Version.xcconfig", "APP_BUILD")',
        'xcconfigValue(',
        '"iosApp/Configuration/MediaPipe.xcconfig"',
        '"IOS_MEDIAPIPE_TASKS_VERSION"',
        "versionCode = appBuild",
        "versionName = appVersion",
    )
    for fragment in required_gradle_fragments:
        if fragment not in gradle:
            raise CandidateIdentityError(f"build version wiring missing: {fragment}")
    if gradle.count("version = iosMediaPipeTasksVersion") != 4:
        raise CandidateIdentityError(
            "all four project-specific iOS MediaPipe pods must use iosMediaPipeTasksVersion"
        )

    if "<string>$(APP_VERSION)</string>" not in info:
        raise CandidateIdentityError("iOS marketing version is not wired to APP_VERSION")
    if "<string>$(APP_BUILD)</string>" not in info:
        raise CandidateIdentityError("iOS build number is not wired to APP_BUILD")
    if "<key>EyespieMediaPipeTasksVersion</key>" not in info or (
        "<string>$(IOS_MEDIAPIPE_TASKS_VERSION)</string>" not in info
    ):
        raise CandidateIdentityError(
            "iOS runtime MediaPipe identity is not wired to IOS_MEDIAPIPE_TASKS_VERSION"
        )
    if "NSLocationWhenInUseUsageDescription" in info:
        raise CandidateIdentityError("stale iOS location usage declaration is present")

    for name, content in (("debug", debug_config), ("release", release_config)):
        if '#include "Version.xcconfig"' not in content:
            raise CandidateIdentityError(f"iOS {name} config does not include Version.xcconfig")
        if '#include "MediaPipe.xcconfig"' not in content:
            raise CandidateIdentityError(f"iOS {name} config does not include MediaPipe.xcconfig")
        if f"BUNDLE_ID={EXPECTED_IOS_BUNDLE_ID}" not in content:
            raise CandidateIdentityError(
                f"iOS {name} config must define canonical BUNDLE_ID={EXPECTED_IOS_BUNDLE_ID}"
            )

    canonical_project_wiring = 'PRODUCT_BUNDLE_IDENTIFIER = "${BUNDLE_ID}";'
    if xcode_project.count(canonical_project_wiring) != 2:
        raise CandidateIdentityError(
            "Xcode Debug/Release project bundle identifiers must inherit canonical BUNDLE_ID"
        )
    if '.ios.${TEAM_ID}' in xcode_project:
        raise CandidateIdentityError(
            "Xcode project must not concatenate signing team identity into the bundle identifier"
        )

    if version == "1.0" and build == 1:
        raise CandidateIdentityError("legacy iOS-only version identity unexpectedly survived")
    if not NUMERIC_VERSION_RE.fullmatch(ios_mediapipe):
        raise CandidateIdentityError("invalid canonical iOS MediaPipe version")


def build_identity(*, allow_dirty: bool) -> dict[str, Any]:
    version_values = version_config_values()
    version = version_values["APP_VERSION"]
    build = int(version_values["APP_BUILD"])
    ios_mediapipe = ios_mediapipe_version()
    verify_wiring(version, build, ios_mediapipe)

    source_sha = git("rev-parse", "HEAD")
    dirty = bool(git("status", "--porcelain"))
    if dirty and not allow_dirty:
        raise CandidateIdentityError(
            "working tree is dirty; release evidence must come from a clean exact source commit"
        )

    versions = tomllib.loads(read_text(VERSIONS_TOML))["versions"]
    gradle = read_text(BUILD_GRADLE)
    core = read_text(CORE_SOURCE)
    embedding = read_text(EMBEDDING_SOURCE)
    bundle = read_text(BUNDLE_SOURCE)

    ios_deployment = require_regex(
        gradle,
        r'ios\.deploymentTarget\s*=\s*"([^"]+)"',
        "iOS deployment target",
    ).group(1)

    model_sha256 = kotlin_string(embedding, "IMAGE_EMBEDDER_MODEL_SHA256")
    if not SHA256_RE.fullmatch(model_sha256):
        raise CandidateIdentityError("embedding model SHA-256 must be 64 lowercase hex characters")

    android_mediapipe = str(versions["mediapipe"])
    android_mediapipe_genai = str(versions["mediapipeGenAI"])

    return {
        "candidate_identity_schema_version": CANDIDATE_IDENTITY_SCHEMA_VERSION,
        "candidate": f"{version}+{build}@{source_sha[:12]}",
        "repository": "ryjen/eyespie",
        "source": {
            "commit_sha": source_sha,
            "dirty": dirty,
        },
        "application": {
            "version": version,
            "build": build,
        },
        "platforms": {
            "android": {
                "min_sdk": int(versions["android-minSdk"]),
                "target_sdk": int(versions["android-targetSdk"]),
            },
            "ios": {
                "deployment_target": ios_deployment,
            },
        },
        "persistence": {
            "sqldelight_schema_version": current_sqldelight_schema_version(),
        },
        "match_policy": {
            "default_cosine_threshold": kotlin_float(core, "DEFAULT_THRESHOLD"),
        },
        "bundle": {
            "schema_version": kotlin_int(bundle, "GAME_BUNDLE_SCHEMA_VERSION"),
            "canonicalization_version": kotlin_int(bundle, "GAME_BUNDLE_CANONICALIZATION_VERSION"),
            "signature_algorithm": kotlin_int(
                bundle,
                "GAME_BUNDLE_SIGNATURE_ALGORITHM_P256_ECDSA_SHA256_DER",
            ),
            "match_policy_version": kotlin_int(bundle, "GAME_BUNDLE_MATCH_POLICY_VERSION"),
        },
        "image_embedding": {
            "contract_version": kotlin_int(embedding, "IMAGE_EMBEDDING_CONTRACT_VERSION"),
            "dimensions": kotlin_int(embedding, "IMAGE_EMBEDDING_DIMENSIONS"),
            "model_id": kotlin_string(embedding, "IMAGE_EMBEDDER_MODEL_ID"),
            "model_file": kotlin_string(embedding, "IMAGE_EMBEDDER_MODEL_FILE"),
            "model_sha256": model_sha256,
        },
        "mediapipe": {
            "android": {
                "tasks_vision": android_mediapipe,
                "tasks_genai": android_mediapipe_genai,
            },
            "ios": {
                "project_artifact_version": ios_mediapipe,
            },
        },
    }


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    verify = subparsers.add_parser("verify", help="verify candidate identity wiring and invariants")
    verify.add_argument("--allow-dirty", action="store_true")

    render = subparsers.add_parser("render", help="render candidate identity JSON")
    render.add_argument("--output", type=Path)
    render.add_argument("--allow-dirty", action="store_true")

    args = parser.parse_args()
    try:
        identity = build_identity(allow_dirty=args.allow_dirty)
    except CandidateIdentityError as exc:
        parser.error(str(exc))

    if args.command == "verify":
        print(
            "verified candidate identity: "
            f"schema={identity['candidate_identity_schema_version']} "
            f"{identity['candidate']} "
            f"db={identity['persistence']['sqldelight_schema_version']} "
            f"bundle={identity['bundle']['schema_version']} "
            f"threshold={identity['match_policy']['default_cosine_threshold']:.9g} "
            f"model={identity['image_embedding']['model_sha256']}"
        )
        return 0

    encoded = json.dumps(identity, indent=2, sort_keys=True) + "\n"
    if args.output:
        args.output.write_text(encoded, encoding="utf-8")
    else:
        sys.stdout.write(encoded)
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
