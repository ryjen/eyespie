#!/usr/bin/env python3
"""Stage immutable non-sensitive MediaPipe fixtures for embedding calibration.

Fixture binaries remain untracked. The manifest pins each GCS object by generation and
SHA-256; this script refuses redirects, unexpected hosts/paths, duplicate names/ids, or
digest mismatches before materializing Android/iOS calibration resources.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import os
import re
import shutil
import tempfile
import urllib.error
import urllib.parse
import urllib.request
from dataclasses import dataclass
from pathlib import Path
from typing import Callable

REPO_ROOT = Path(__file__).resolve().parents[1]
DEFAULT_MANIFEST = REPO_ROOT / "calibration" / "image-embedding-fixtures.json"
EXPECTED_HOST = "storage.googleapis.com"
EXPECTED_PATH_PREFIX = "/mediapipe-assets/tasks/testdata/vision/"
EXPECTED_IDS = ("burger", "burger_crop", "burger_rotated", "cat")
EXPECTED_ROLES = {
    "burger": "reference",
    "burger_crop": "related",
    "burger_rotated": "related",
    "cat": "unrelated",
}
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
REVISION_RE = re.compile(r"^[0-9a-f]{40}$")
FILE_NAME_RE = re.compile(r"^[a-z0-9_]+\.jpg$")
RUNTIME_MANIFEST_NAME = "manifest.json"


class FixtureArtifactError(RuntimeError):
    """Raised when fixture provenance, staging, or verification fails."""


@dataclass(frozen=True)
class Fixture:
    fixture_id: str
    role: str
    file_name: str
    sha256: str
    source_url: str


@dataclass(frozen=True)
class FixtureManifest:
    source_repository: str
    source_revision: str
    license_spdx: str
    fixtures: tuple[Fixture, ...]


def _validate_source_url(url: str, file_name: str) -> None:
    if not isinstance(url, str):
        raise FixtureArtifactError("fixture URL must be a string")
    parsed = urllib.parse.urlparse(url)
    try:
        query = urllib.parse.parse_qs(parsed.query, strict_parsing=True)
    except ValueError as exc:
        raise FixtureArtifactError("fixture source query is malformed") from exc

    generation = query.get("generation", [])
    expected_path = f"{EXPECTED_PATH_PREFIX}{file_name}"
    if parsed.scheme != "https":
        raise FixtureArtifactError("fixture source must use HTTPS")
    if parsed.hostname != EXPECTED_HOST or parsed.port is not None:
        raise FixtureArtifactError("fixture source must use the approved storage.googleapis.com host")
    if parsed.path != expected_path:
        raise FixtureArtifactError(f"fixture source path does not match {file_name}")
    if parsed.params or parsed.fragment:
        raise FixtureArtifactError("fixture source must not contain params or a fragment")
    if set(query) != {"generation"} or len(generation) != 1 or not generation[0].isdigit():
        raise FixtureArtifactError("fixture source must pin exactly one numeric GCS generation")


def load_manifest(path: Path = DEFAULT_MANIFEST) -> FixtureManifest:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise FixtureArtifactError(f"cannot read fixture manifest: {path}") from exc

    try:
        schema_version = payload["schema_version"]
        source_repository = payload["source_repository"]
        source_revision = payload["source_revision"]
        license_spdx = payload["license_spdx"]
        raw_fixtures = payload["fixtures"]
    except (KeyError, TypeError) as exc:
        raise FixtureArtifactError("fixture manifest is missing required fields") from exc

    if schema_version != 1:
        raise FixtureArtifactError(f"unsupported fixture manifest schema: {schema_version!r}")
    if source_repository != "https://github.com/ryjen/mediapipe":
        raise FixtureArtifactError("unexpected fixture source repository")
    if not isinstance(source_revision, str) or not REVISION_RE.fullmatch(source_revision):
        raise FixtureArtifactError("source_revision must be a 40-character lowercase Git SHA")
    if license_spdx != "Apache-2.0":
        raise FixtureArtifactError("fixture license must remain Apache-2.0")
    if not isinstance(raw_fixtures, list):
        raise FixtureArtifactError("fixtures must be a list")

    fixtures: list[Fixture] = []
    seen_ids: set[str] = set()
    seen_names: set[str] = set()
    for item in raw_fixtures:
        try:
            fixture_id = item["id"]
            role = item["role"]
            file_name = item["file_name"]
            digest = item["sha256"]
            source_url = item["url"]
        except (KeyError, TypeError) as exc:
            raise FixtureArtifactError("fixture entry is missing required fields") from exc

        if fixture_id not in EXPECTED_ROLES or role != EXPECTED_ROLES[fixture_id]:
            raise FixtureArtifactError(f"unexpected fixture identity/role: {fixture_id!r}/{role!r}")
        if fixture_id in seen_ids:
            raise FixtureArtifactError(f"duplicate fixture id: {fixture_id}")
        if not isinstance(file_name, str) or not FILE_NAME_RE.fullmatch(file_name):
            raise FixtureArtifactError(f"invalid fixture filename: {file_name!r}")
        if file_name in seen_names:
            raise FixtureArtifactError(f"duplicate fixture filename: {file_name}")
        if not isinstance(digest, str) or not SHA256_RE.fullmatch(digest):
            raise FixtureArtifactError(f"invalid SHA-256 for fixture {fixture_id}")
        _validate_source_url(source_url, file_name)

        seen_ids.add(fixture_id)
        seen_names.add(file_name)
        fixtures.append(Fixture(fixture_id, role, file_name, digest, source_url))

    if tuple(f.fixture_id for f in fixtures) != EXPECTED_IDS:
        raise FixtureArtifactError(f"fixture ids/order must be {', '.join(EXPECTED_IDS)}")

    return FixtureManifest(source_repository, source_revision, license_spdx, tuple(fixtures))


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise FixtureArtifactError(f"cannot read fixture: {path}") from exc
    return digest.hexdigest()


def verify_file(path: Path, fixture: Fixture) -> None:
    if not path.is_file():
        raise FixtureArtifactError(f"fixture is missing: {path}")
    actual = sha256_file(path)
    if actual != fixture.sha256:
        raise FixtureArtifactError(
            f"fixture SHA-256 mismatch for {path}: expected {fixture.sha256}, got {actual}"
        )


def download_fixture(url: str, destination: Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": "eyespie-fixture-stager/1"})
    try:
        with urllib.request.urlopen(request, timeout=120) as response, destination.open("wb") as output:
            if response.geturl() != url:
                raise FixtureArtifactError("fixture source redirected away from the generation-pinned URL")
            shutil.copyfileobj(response, output, length=1024 * 1024)
    except FixtureArtifactError:
        raise
    except (OSError, urllib.error.URLError) as exc:
        raise FixtureArtifactError("failed to download a pinned image-embedding fixture") from exc


def target_directory(target: str) -> Path:
    if target == "android":
        return REPO_ROOT / "eyespie" / "src" / "androidInstrumentedTest" / "assets" / "image-embedding-calibration"
    if target == "ios":
        return REPO_ROOT / "iosApp" / "CalibrationFixtures"
    raise FixtureArtifactError(f"unsupported target: {target}")


def selected_targets(target: str) -> tuple[str, ...]:
    return ("android", "ios") if target == "all" else (target,)


def stage_fixture(
    fixture: Fixture,
    destination: Path,
    *,
    downloader: Callable[[str, Path], None] = download_fixture,
) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)
    if destination.is_file():
        try:
            verify_file(destination, fixture)
            return
        except FixtureArtifactError:
            pass

    fd, temporary_name = tempfile.mkstemp(
        prefix=f".{fixture.file_name}.", suffix=".tmp", dir=destination.parent
    )
    os.close(fd)
    temporary = Path(temporary_name)
    try:
        downloader(fixture.source_url, temporary)
        verify_file(temporary, fixture)
        os.replace(temporary, destination)
        verify_file(destination, fixture)
    finally:
        temporary.unlink(missing_ok=True)


def runtime_manifest_bytes(manifest: FixtureManifest) -> bytes:
    payload = {
        "schema_version": 1,
        "fixtures": [
            {
                "id": fixture.fixture_id,
                "role": fixture.role,
                "file_name": fixture.file_name,
                "sha256": fixture.sha256,
            }
            for fixture in manifest.fixtures
        ],
    }
    return (json.dumps(payload, sort_keys=True, separators=(",", ":")) + "\n").encode("utf-8")


def write_runtime_manifest(manifest: FixtureManifest, directory: Path) -> None:
    directory.mkdir(parents=True, exist_ok=True)
    (directory / RUNTIME_MANIFEST_NAME).write_bytes(runtime_manifest_bytes(manifest))


def verify_runtime_manifest(manifest: FixtureManifest, directory: Path) -> None:
    path = directory / RUNTIME_MANIFEST_NAME
    try:
        actual = path.read_bytes()
    except OSError as exc:
        raise FixtureArtifactError(f"runtime fixture manifest is missing: {path}") from exc
    if actual != runtime_manifest_bytes(manifest):
        raise FixtureArtifactError("runtime fixture manifest does not match validated provenance")


def stage_target(manifest: FixtureManifest, target: str) -> None:
    directory = target_directory(target)
    for fixture in manifest.fixtures:
        stage_fixture(fixture, directory / fixture.file_name)
    write_runtime_manifest(manifest, directory)


def verify_target(manifest: FixtureManifest, target: str) -> None:
    directory = target_directory(target)
    for fixture in manifest.fixtures:
        verify_file(directory / fixture.file_name, fixture)
    verify_runtime_manifest(manifest, directory)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    subparsers = parser.add_subparsers(dest="command", required=True)
    subparsers.add_parser("validate-manifest", help="validate fixture provenance metadata")

    stage = subparsers.add_parser("stage", help="download and verify fixture bytes")
    stage.add_argument("--target", choices=("android", "ios", "all"), required=True)

    verify = subparsers.add_parser("verify", help="verify already-staged fixture bytes")
    verify.add_argument("--target", choices=("android", "ios", "all"), required=True)

    args = parser.parse_args()
    try:
        manifest = load_manifest(args.manifest)
        if args.command == "validate-manifest":
            print(f"validated fixture manifest: {args.manifest}")
        elif args.command == "stage":
            for target in selected_targets(args.target):
                stage_target(manifest, target)
                print(f"verified {target} calibration fixtures: {target_directory(target)}")
        else:
            for target in selected_targets(args.target):
                verify_target(manifest, target)
                print(f"verified {target} calibration fixtures: {target_directory(target)}")
    except FixtureArtifactError as exc:
        parser.error(str(exc))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
