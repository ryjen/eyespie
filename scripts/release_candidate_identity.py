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
BUILD_GRADLE = ROOT / "eyespie/build.gradle.kts"
INFO_PLIST = ROOT / "iosApp/iosApp/Info.plist"
DEBUG_XCCONFIG = ROOT / "iosApp/Configuration/Config.debug.xcconfig"
RELEASE_XCCONFIG = ROOT / "iosApp/Configuration/Config.release.xcconfig"
VERSIONS_TOML = ROOT / "gradle/libs.versions.toml"
EMBEDDING_SOURCE = ROOT / "eyespie/src/commonMain/kotlin/com/micrantha/eyespie/imaging/ImageEmbedding.kt"
BUNDLE_SOURCE = ROOT / "eyespie/src/commonMain/kotlin/com/micrantha/eyespie/sharing/GameBundle.kt"
SQLDELIGHT_DIR = ROOT / "eyespie/src/commonMain/sqldelight/com/micrantha/eyespie/data"

SEMVER_RE = re.compile(r"^[0-9]+\.[0-9]+\.[0-9]+(?:-[0-9A-Za-z.-]+)?$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


class CandidateIdentityError(ValueError):
    pass


def read_text(path: Path) -> str:
    try:
        return path.read_text(encoding="utf-8")
    except OSError as exc:
        raise CandidateIdentityError(f"cannot read required file: {path.relative_to(ROOT)}") from exc


def xcconfig_values() -> dict[str, str]:
    values: dict[str, str] = {}
    for raw_line in read_text(VERSION_CONFIG).splitlines():
        line = raw_line.strip()
        if not line or line.startswith("//"):
            continue
        if "=" not in line:
            raise CandidateIdentityError("Version.xcconfig contains a non-assignment line")
        key, value = (part.strip() for part in line.split("=", 1))
        if not key or not value:
            raise CandidateIdentityError("Version.xcconfig contains an empty key/value")
        if key in values:
            raise CandidateIdentityError(f"duplicate version key: {key}")
        values[key] = value
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


def require_regex(text: str, pattern: str, description: str) -> re.Match[str]:
    match = re.search(pattern, text, flags=re.MULTILINE | re.DOTALL)
    if match is None:
        raise CandidateIdentityError(f"cannot derive {description}")
    return match


def kotlin_int(text: str, name: str) -> int:
    match = require_regex(text, rf"const val {re.escape(name)}\s*=\s*([0-9]+)", name)
    return int(match.group(1))


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


def verify_wiring(version: str, build: int) -> None:
    gradle = read_text(BUILD_GRADLE)
    info = read_text(INFO_PLIST)
    debug_config = read_text(DEBUG_XCCONFIG)
    release_config = read_text(RELEASE_XCCONFIG)

    required_gradle_fragments = (
        'versionConfigValue("APP_VERSION")',
        'versionConfigValue("APP_BUILD")',
        "versionCode = appBuild",
        "versionName = appVersion",
    )
    for fragment in required_gradle_fragments:
        if fragment not in gradle:
            raise CandidateIdentityError(f"Android version wiring missing: {fragment}")

    if "<string>$(APP_VERSION)</string>" not in info:
        raise CandidateIdentityError("iOS marketing version is not wired to APP_VERSION")
    if "<string>$(APP_BUILD)</string>" not in info:
        raise CandidateIdentityError("iOS build number is not wired to APP_BUILD")
    if "NSLocationWhenInUseUsageDescription" in info:
        raise CandidateIdentityError("stale iOS location usage declaration is present")

    for name, content in (("debug", debug_config), ("release", release_config)):
        if '#include "Version.xcconfig"' not in content:
            raise CandidateIdentityError(f"iOS {name} config does not include Version.xcconfig")

    if version == "1.0" and build == 1:
        raise CandidateIdentityError("legacy iOS-only version identity unexpectedly survived")


def build_identity(*, allow_dirty: bool) -> dict[str, Any]:
    version_values = xcconfig_values()
    version = version_values["APP_VERSION"]
    build = int(version_values["APP_BUILD"])
    verify_wiring(version, build)

    source_sha = git("rev-parse", "HEAD")
    dirty = bool(git("status", "--porcelain"))
    if dirty and not allow_dirty:
        raise CandidateIdentityError(
            "working tree is dirty; release evidence must come from a clean exact source commit"
        )

    versions = tomllib.loads(read_text(VERSIONS_TOML))["versions"]
    gradle = read_text(BUILD_GRADLE)
    embedding = read_text(EMBEDDING_SOURCE)
    bundle = read_text(BUNDLE_SOURCE)

    ios_versions = set(
        re.findall(
            r'pod\("EyespieMediaPipe[^\"]+"\)\s*\{\s*version\s*=\s*"([^\"]+)"',
            gradle,
            flags=re.MULTILINE,
        )
    )
    if len(ios_versions) != 1:
        raise CandidateIdentityError("project-specific iOS MediaPipe pods must share one version")
    ios_mediapipe = next(iter(ios_versions))

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
        "candidate_identity_schema_version": 1,
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
        "bundle": {
            "schema_version": kotlin_int(bundle, "GAME_BUNDLE_SCHEMA_VERSION"),
            "canonicalization_version": kotlin_int(bundle, "GAME_BUNDLE_CANONICALIZATION_VERSION"),
            "signature_algorithm": kotlin_int(
                bundle,
                "GAME_BUNDLE_SIGNATURE_ALGORITHM_P256_ECDSA_SHA256_DER",
            ),
            "match_policy_version": kotlin_int(bundle, "GAME_BUNDLE_MATCH_POLICY_VERSION"),
            "max_bytes": kotlin_int(bundle, "GAME_BUNDLE_MAX_BYTES"),
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
            f"{identity['candidate']} "
            f"db={identity['persistence']['sqldelight_schema_version']} "
            f"bundle={identity['bundle']['schema_version']} "
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
