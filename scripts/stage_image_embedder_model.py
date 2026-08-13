#!/usr/bin/env python3
"""Stage and verify the mandatory image-embedding model artifact.

The model URL is generation-pinned and its bytes are accepted only when they
match the SHA-256 recorded in models/image-embedder.json. Generated model bytes
remain untracked; the manifest is the source of truth.
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
DEFAULT_MANIFEST = REPO_ROOT / "models" / "image-embedder.json"
EXPECTED_FILE_NAME = "mobilenet_v3_small_100_224_embedder.tflite"
EXPECTED_DIMENSION = 1024
EXPECTED_HOST = "storage.googleapis.com"
EXPECTED_PATH = f"/mediapipe-assets/tasks/testdata/vision/{EXPECTED_FILE_NAME}"
SHA256_RE = re.compile(r"^[0-9a-f]{64}$")
REVISION_RE = re.compile(r"^[0-9a-f]{40}$")


class ModelArtifactError(RuntimeError):
    """Raised when model provenance, staging, or verification fails."""


@dataclass(frozen=True)
class ModelManifest:
    model_id: str
    file_name: str
    sha256: str
    embedding_dimension: int
    source_url: str
    source_revision: str


def load_manifest(path: Path = DEFAULT_MANIFEST) -> ModelManifest:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise ModelArtifactError(f"cannot read model manifest: {path}") from exc

    try:
        schema_version = payload["schema_version"]
        model_id = payload["model_id"]
        file_name = payload["file_name"]
        digest = payload["sha256"]
        dimension = payload["embedding_dimension"]
        source = payload["source"]
        source_url = source["url"]
        source_revision = source["manifest_revision"]
    except (KeyError, TypeError) as exc:
        raise ModelArtifactError("model manifest is missing required fields") from exc

    if schema_version != 1:
        raise ModelArtifactError(f"unsupported model manifest schema: {schema_version!r}")
    if not isinstance(model_id, str) or not model_id.strip():
        raise ModelArtifactError("model_id must be a non-empty string")
    if file_name != EXPECTED_FILE_NAME:
        raise ModelArtifactError(f"unexpected model filename: {file_name!r}")
    if dimension != EXPECTED_DIMENSION:
        raise ModelArtifactError(f"unexpected embedding dimension: {dimension!r}")
    if not isinstance(digest, str) or not SHA256_RE.fullmatch(digest):
        raise ModelArtifactError("sha256 must be exactly 64 lowercase hexadecimal characters")
    if not isinstance(source_revision, str) or not REVISION_RE.fullmatch(source_revision):
        raise ModelArtifactError("manifest_revision must be a 40-character lowercase Git SHA")
    validate_source_url(source_url)

    return ModelManifest(
        model_id=model_id,
        file_name=file_name,
        sha256=digest,
        embedding_dimension=dimension,
        source_url=source_url,
        source_revision=source_revision,
    )


def validate_source_url(url: str) -> None:
    if not isinstance(url, str):
        raise ModelArtifactError("source URL must be a string")

    parsed = urllib.parse.urlparse(url)
    try:
        query = urllib.parse.parse_qs(parsed.query, strict_parsing=True)
    except ValueError as exc:
        raise ModelArtifactError("model source query is malformed") from exc
    generation = query.get("generation", [])

    if parsed.scheme != "https":
        raise ModelArtifactError("model source must use HTTPS")
    if parsed.hostname != EXPECTED_HOST or parsed.port is not None:
        raise ModelArtifactError("model source must use the approved storage.googleapis.com host")
    if parsed.path != EXPECTED_PATH:
        raise ModelArtifactError("model source path does not match the approved artifact")
    if parsed.params or parsed.fragment:
        raise ModelArtifactError("model source must not contain params or a fragment")
    if set(query) != {"generation"} or len(generation) != 1 or not generation[0].isdigit():
        raise ModelArtifactError("model source must pin exactly one numeric GCS generation")


def sha256_file(path: Path) -> str:
    digest = hashlib.sha256()
    try:
        with path.open("rb") as stream:
            for chunk in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(chunk)
    except OSError as exc:
        raise ModelArtifactError(f"cannot read model artifact: {path}") from exc
    return digest.hexdigest()


def verify_file(path: Path, manifest: ModelManifest) -> None:
    if not path.is_file():
        raise ModelArtifactError(f"model artifact is missing: {path}")
    actual = sha256_file(path)
    if actual != manifest.sha256:
        raise ModelArtifactError(
            f"model SHA-256 mismatch for {path}: expected {manifest.sha256}, got {actual}"
        )


def download_model(url: str, destination: Path) -> None:
    request = urllib.request.Request(url, headers={"User-Agent": "eyespie-model-stager/1"})
    try:
        with urllib.request.urlopen(request, timeout=120) as response, destination.open("wb") as output:
            if response.geturl() != url:
                raise ModelArtifactError("model source redirected away from the generation-pinned URL")
            shutil.copyfileobj(response, output, length=1024 * 1024)
    except ModelArtifactError:
        raise
    except (OSError, urllib.error.URLError) as exc:
        raise ModelArtifactError("failed to download the pinned image-embedder model") from exc


def stage_model(
    manifest: ModelManifest,
    destination: Path,
    *,
    source_file: Path | None = None,
    downloader: Callable[[str, Path], None] = download_model,
) -> None:
    destination.parent.mkdir(parents=True, exist_ok=True)

    if destination.is_file():
        try:
            verify_file(destination, manifest)
            return
        except ModelArtifactError:
            pass

    fd, temporary_name = tempfile.mkstemp(
        prefix=f".{manifest.file_name}.", suffix=".tmp", dir=destination.parent
    )
    os.close(fd)
    temporary = Path(temporary_name)
    try:
        if source_file is not None:
            if not source_file.is_file():
                raise ModelArtifactError(f"source model is missing: {source_file}")
            shutil.copyfile(source_file, temporary)
        else:
            downloader(manifest.source_url, temporary)

        verify_file(temporary, manifest)
        os.replace(temporary, destination)
        verify_file(destination, manifest)
    finally:
        temporary.unlink(missing_ok=True)


def target_path(target: str, manifest: ModelManifest) -> Path:
    if target == "android":
        return REPO_ROOT / "eyespie" / "src" / "androidMain" / "assets" / manifest.file_name
    if target == "ios":
        return REPO_ROOT / "iosApp" / "ModelArtifacts" / manifest.file_name
    raise ModelArtifactError(f"unsupported target: {target}")


def selected_targets(target: str) -> tuple[str, ...]:
    return ("android", "ios") if target == "all" else (target,)


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, default=DEFAULT_MANIFEST)
    subparsers = parser.add_subparsers(dest="command", required=True)

    subparsers.add_parser("validate-manifest", help="validate provenance metadata")

    stage = subparsers.add_parser("stage", help="stage verified model bytes")
    stage.add_argument("--target", choices=("android", "ios", "all"), required=True)
    stage.add_argument("--source-file", type=Path)

    verify = subparsers.add_parser("verify", help="verify a staged target")
    verify.add_argument("--target", choices=("android", "ios", "all"), required=True)

    verify_file_parser = subparsers.add_parser("verify-file", help="verify an explicit file")
    verify_file_parser.add_argument("path", type=Path)

    args = parser.parse_args()
    try:
        manifest = load_manifest(args.manifest)
        if args.command == "validate-manifest":
            print(f"validated model manifest: {args.manifest}")
        elif args.command == "stage":
            targets = selected_targets(args.target)
            first_staged: Path | None = args.source_file
            for target in targets:
                destination = target_path(target, manifest)
                stage_model(manifest, destination, source_file=first_staged)
                if first_staged is None:
                    first_staged = destination
                print(f"verified {target} model: {destination}")
        elif args.command == "verify":
            for target in selected_targets(args.target):
                destination = target_path(target, manifest)
                verify_file(destination, manifest)
                print(f"verified {target} model: {destination}")
        else:
            verify_file(args.path, manifest)
            print(f"verified model: {args.path}")
    except ModelArtifactError as exc:
        parser.error(str(exc))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
