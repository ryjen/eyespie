#!/usr/bin/env python3
"""Validate bounded closed-alpha internal-distribution artifact evidence.

This tool never signs or uploads artifacts. It binds already-built artifacts to the exact
candidate manifest and rejects package/version/runtime drift before a protected upload step.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import subprocess
import sys
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
EXPECTED_REPOSITORY = "ryjen/eyespie"
EXPECTED_ANDROID_PACKAGE = "com.micrantha.eyespie"
EXPECTED_IOS_BUNDLE_ID = "com.micrantha.eyespie"
CANDIDATE_SCHEMA_VERSION = 2
SHA_RE = re.compile(r"^[0-9a-f]{40}$")
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")


class ReleaseEvidenceError(ValueError):
    pass


def _read_json(path: Path) -> dict[str, Any]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ReleaseEvidenceError(f"cannot read JSON evidence: {path.name}") from exc
    if not isinstance(payload, dict):
        raise ReleaseEvidenceError(f"JSON evidence must be an object: {path.name}")
    return payload


def _sha256(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise ReleaseEvidenceError(f"cannot read artifact: {path.name}") from exc
    return digest.hexdigest()


def _candidate(path: Path) -> dict[str, Any]:
    payload = _read_json(path)
    if payload.get("candidate_identity_schema_version") != CANDIDATE_SCHEMA_VERSION:
        raise ReleaseEvidenceError("unsupported candidate identity schema")
    if payload.get("repository") != EXPECTED_REPOSITORY:
        raise ReleaseEvidenceError("candidate repository does not match Eyespie")
    source = payload.get("source")
    application = payload.get("application")
    mediapipe = payload.get("mediapipe")
    if not isinstance(source, dict) or not isinstance(application, dict) or not isinstance(mediapipe, dict):
        raise ReleaseEvidenceError("candidate identity is missing required sections")
    sha = source.get("commit_sha")
    if not isinstance(sha, str) or not SHA_RE.fullmatch(sha):
        raise ReleaseEvidenceError("candidate source commit must be a full lowercase Git SHA")
    if source.get("dirty") is not False:
        raise ReleaseEvidenceError("candidate identity must come from a clean tree")
    version = application.get("version")
    build = application.get("build")
    if not isinstance(version, str) or not version:
        raise ReleaseEvidenceError("candidate application version is missing")
    if not isinstance(build, int) or build <= 0:
        raise ReleaseEvidenceError("candidate application build must be positive")
    return payload


def _git_head() -> str:
    try:
        return subprocess.check_output(
            ["git", "-C", str(ROOT), "rev-parse", "HEAD"],
            text=True,
            stderr=subprocess.DEVNULL,
        ).strip()
    except (OSError, subprocess.CalledProcessError) as exc:
        raise ReleaseEvidenceError("git metadata is unavailable") from exc


def verify_source(candidate_path: Path, expected_sha: str) -> dict[str, Any]:
    if not SHA_RE.fullmatch(expected_sha):
        raise ReleaseEvidenceError("expected source SHA must be 40 lowercase hex characters")
    candidate = _candidate(candidate_path)
    candidate_sha = candidate["source"]["commit_sha"]
    if candidate_sha != expected_sha:
        raise ReleaseEvidenceError("candidate manifest source SHA does not match requested source SHA")
    if _git_head() != expected_sha:
        raise ReleaseEvidenceError("checked-out HEAD does not match requested source SHA")
    return candidate


def _metadata(path: Path) -> dict[str, Any]:
    payload = _read_json(path)
    if len(json.dumps(payload, separators=(",", ":"))) > 16 * 1024:
        raise ReleaseEvidenceError("artifact metadata exceeds bounded release evidence size")
    return payload


def _base_evidence(candidate: dict[str, Any], platform: str) -> dict[str, Any]:
    return {
        "release_evidence_schema_version": 1,
        "candidate": candidate["candidate"],
        "source_sha": candidate["source"]["commit_sha"],
        "application": {
            "version": candidate["application"]["version"],
            "build": candidate["application"]["build"],
        },
        "platform": platform,
    }


def validate_android(candidate_path: Path, metadata_path: Path, apk: Path, aab: Path) -> dict[str, Any]:
    candidate = _candidate(candidate_path)
    metadata = _metadata(metadata_path)
    if metadata.get("package_id") != EXPECTED_ANDROID_PACKAGE:
        raise ReleaseEvidenceError("Android package id does not match the closed-alpha application")
    if metadata.get("version") != candidate["application"]["version"]:
        raise ReleaseEvidenceError("Android version does not match candidate identity")
    if metadata.get("build") != candidate["application"]["build"]:
        raise ReleaseEvidenceError("Android version code does not match candidate identity")
    permissions = metadata.get("permissions")
    if not isinstance(permissions, list) or not all(isinstance(value, str) for value in permissions):
        raise ReleaseEvidenceError("Android permissions must be a string list")
    if "android.permission.INTERNET" in permissions:
        raise ReleaseEvidenceError("closed-alpha Android artifact unexpectedly requests INTERNET")
    if len(permissions) > 64:
        raise ReleaseEvidenceError("Android permission evidence is unexpectedly large")

    evidence = _base_evidence(candidate, "android")
    evidence.update(
        {
            "package_id": EXPECTED_ANDROID_PACKAGE,
            "permissions": sorted(set(permissions)),
            "artifacts": {
                "apk": {"sha256": _sha256(apk)},
                "aab": {"sha256": _sha256(aab)},
            },
            "channel": "play-internal",
        }
    )
    return evidence


def validate_ios(candidate_path: Path, metadata_path: Path, ipa: Path) -> dict[str, Any]:
    candidate = _candidate(candidate_path)
    metadata = _metadata(metadata_path)
    if metadata.get("bundle_id") != EXPECTED_IOS_BUNDLE_ID:
        raise ReleaseEvidenceError("iOS bundle id does not match the closed-alpha application")
    if metadata.get("version") != candidate["application"]["version"]:
        raise ReleaseEvidenceError("iOS marketing version does not match candidate identity")
    expected_build = str(candidate["application"]["build"])
    if str(metadata.get("build")) != expected_build:
        raise ReleaseEvidenceError("iOS build number does not match candidate identity")
    ios_runtime = candidate.get("mediapipe", {}).get("ios", {})
    if metadata.get("mediapipe_version") != ios_runtime.get("project_artifact_version"):
        raise ReleaseEvidenceError("iOS MediaPipe artifact identity does not match candidate identity")

    evidence = _base_evidence(candidate, "ios")
    evidence.update(
        {
            "bundle_id": EXPECTED_IOS_BUNDLE_ID,
            "mediapipe_version": metadata["mediapipe_version"],
            "artifacts": {"ipa": {"sha256": _sha256(ipa)}},
            "channel": "testflight-internal",
        }
    )
    return evidence


def _write(payload: dict[str, Any], output: Path | None) -> None:
    encoded = json.dumps(payload, indent=2, sort_keys=True) + "\n"
    if output is None:
        sys.stdout.write(encoded)
    else:
        output.parent.mkdir(parents=True, exist_ok=True)
        output.write_text(encoded, encoding="utf-8")


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    source = subparsers.add_parser("verify-source")
    source.add_argument("--candidate", type=Path, required=True)
    source.add_argument("--expected-sha", required=True)

    android = subparsers.add_parser("android")
    android.add_argument("--candidate", type=Path, required=True)
    android.add_argument("--metadata", type=Path, required=True)
    android.add_argument("--apk", type=Path, required=True)
    android.add_argument("--aab", type=Path, required=True)
    android.add_argument("--output", type=Path)

    ios = subparsers.add_parser("ios")
    ios.add_argument("--candidate", type=Path, required=True)
    ios.add_argument("--metadata", type=Path, required=True)
    ios.add_argument("--ipa", type=Path, required=True)
    ios.add_argument("--output", type=Path)

    args = parser.parse_args()
    try:
        if args.command == "verify-source":
            candidate = verify_source(args.candidate, args.expected_sha)
            print(f"verified internal distribution source: {candidate['candidate']}")
        elif args.command == "android":
            _write(validate_android(args.candidate, args.metadata, args.apk, args.aab), args.output)
        else:
            _write(validate_ios(args.candidate, args.metadata, args.ipa), args.output)
    except ReleaseEvidenceError as exc:
        parser.error(str(exc))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
