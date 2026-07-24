#!/usr/bin/env python3
"""Stage and verify the Android Play Asset Delivery model artifact.

The large model is intentionally not committed to Git. This tool copies an approved
MediaPipe `.task` file into the asset-pack working tree, calculates its immutable
identity, writes the runtime manifest, and updates the Android descriptor constants.
"""

from __future__ import annotations

import argparse
import hashlib
import json
import re
import shutil
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
MODEL_DIR = ROOT / "model-pack/src/main/assets/model"
MANIFEST_PATH = MODEL_DIR / "manifest.json"
MODULE_PATH = ROOT / "eyespie/src/androidMain/kotlin/com/micrantha/eyespie/model/Module.android.kt"

DESCRIPTOR_PATTERN = re.compile(
    r'internal val androidSmokeModelDescriptor = ModelAssetDescriptor\(.*?\n\)',
    re.DOTALL,
)
RELEASE_ID_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
ARTIFACT_NAME_PATTERN = re.compile(r"^[A-Za-z0-9][A-Za-z0-9._-]*\.task$")


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def validate_release_value(name: str, value: str) -> None:
    if not RELEASE_ID_PATTERN.fullmatch(value):
        raise SystemExit(
            f"{name} must contain only letters, numbers, '.', '_', and '-' "
            "and must be at most 128 characters"
        )


def validate_artifact_name(filename: str) -> None:
    if not ARTIFACT_NAME_PATTERN.fullmatch(filename):
        raise SystemExit(
            "Artifact filename must be a simple .task basename containing only "
            "letters, numbers, '.', '_', and '-'"
        )


def staged_task_files() -> list[Path]:
    if not MODEL_DIR.is_dir():
        return []
    return sorted(path for path in MODEL_DIR.glob("*.task") if path.is_file())


def descriptor_source(
    model_id: str,
    version: str,
    filename: str,
    size_bytes: int,
    digest: str,
    minimum_runtime_version: str,
    minimum_model_abi: int,
) -> str:
    return f'''internal val androidSmokeModelDescriptor = ModelAssetDescriptor(
    id = "{model_id}",
    version = "{version}",
    filename = "{filename}",
    expectedBytes = {size_bytes}L,
    sha256 = "{digest}",
    runtime = ModelRuntimeCompatibility(
        engine = "mediapipe",
        minimumRuntimeVersion = "{minimum_runtime_version}",
        minimumModelAbi = {minimum_model_abi},
    ),
)'''


def stage(args: argparse.Namespace) -> None:
    source = args.artifact.resolve()
    if not source.is_file():
        raise SystemExit(f"Artifact does not exist: {source}")
    validate_artifact_name(source.name)
    validate_release_value("model ID", args.model_id)
    validate_release_value("version", args.version)
    validate_release_value("minimum runtime version", args.minimum_runtime_version)
    if args.minimum_model_abi < 1:
        raise SystemExit("minimum model ABI must be at least 1")

    MODEL_DIR.mkdir(parents=True, exist_ok=True)
    destination = MODEL_DIR / source.name

    for existing in staged_task_files():
        if existing.resolve() != destination.resolve():
            existing.unlink()

    if source != destination.resolve():
        shutil.copyfile(source, destination)

    size_bytes = destination.stat().st_size
    digest = sha256(destination)
    manifest = {
        "schemaVersion": 1,
        "modelId": args.model_id,
        "version": args.version,
        "filename": destination.name,
        "sizeBytes": size_bytes,
        "sha256": digest,
        "runtime": {
            "engine": "mediapipe",
            "minimumRuntimeVersion": args.minimum_runtime_version,
            "minimumModelAbi": args.minimum_model_abi,
        },
    }
    MANIFEST_PATH.write_text(json.dumps(manifest, indent=2) + "\n", encoding="utf-8")

    module = MODULE_PATH.read_text(encoding="utf-8")
    replacement = descriptor_source(
        model_id=args.model_id,
        version=args.version,
        filename=destination.name,
        size_bytes=size_bytes,
        digest=digest,
        minimum_runtime_version=args.minimum_runtime_version,
        minimum_model_abi=args.minimum_model_abi,
    )
    updated, count = DESCRIPTOR_PATTERN.subn(replacement, module, count=1)
    if count != 1:
        raise SystemExit(f"Could not update androidSmokeModelDescriptor in {MODULE_PATH}")
    MODULE_PATH.write_text(updated, encoding="utf-8")

    print(f"Staged {destination.relative_to(ROOT)}")
    print(f"sizeBytes={size_bytes}")
    print(f"sha256={digest}")
    print("Review and commit manifest/descriptor changes, but do not commit the .task file.")


def parse_descriptor(module: str) -> dict[str, object]:
    match = DESCRIPTOR_PATTERN.search(module)
    if not match:
        raise SystemExit("androidSmokeModelDescriptor was not found")
    block = match.group(0)

    def string(name: str) -> str:
        found = re.search(rf'{name} = "([^"]+)"', block)
        if not found:
            raise SystemExit(f"Descriptor field missing: {name}")
        return found.group(1)

    def integer(name: str) -> int:
        found = re.search(rf"{name} = (\d+)L?", block)
        if not found:
            raise SystemExit(f"Descriptor field missing: {name}")
        return int(found.group(1))

    return {
        "modelId": string("id"),
        "version": string("version"),
        "filename": string("filename"),
        "sizeBytes": integer("expectedBytes"),
        "sha256": string("sha256"),
        "engine": string("engine"),
        "minimumRuntimeVersion": string("minimumRuntimeVersion"),
        "minimumModelAbi": integer("minimumModelAbi"),
    }


def verify(require_artifact: bool) -> None:
    manifest = json.loads(MANIFEST_PATH.read_text(encoding="utf-8"))
    descriptor = parse_descriptor(MODULE_PATH.read_text(encoding="utf-8"))
    runtime = manifest.get("runtime", {})
    expected = {
        "modelId": manifest.get("modelId"),
        "version": manifest.get("version"),
        "filename": manifest.get("filename"),
        "sizeBytes": manifest.get("sizeBytes"),
        "sha256": manifest.get("sha256"),
        "engine": runtime.get("engine"),
        "minimumRuntimeVersion": runtime.get("minimumRuntimeVersion"),
        "minimumModelAbi": runtime.get("minimumModelAbi"),
    }
    if descriptor != expected:
        print("Descriptor and manifest differ:", file=sys.stderr)
        print(json.dumps({"descriptor": descriptor, "manifest": expected}, indent=2), file=sys.stderr)
        raise SystemExit(1)

    filename = str(manifest["filename"])
    validate_artifact_name(filename)
    task_files = staged_task_files()
    artifact = MODEL_DIR / filename

    if not task_files:
        if require_artifact:
            raise SystemExit(f"Required model artifact is missing: {artifact}")
        print("Descriptor and manifest agree; model artifact is not staged (expected in source CI).")
        return

    if len(task_files) != 1 or task_files[0].name != filename:
        staged = ", ".join(path.name for path in task_files)
        raise SystemExit(
            f"Asset pack must contain exactly one .task file named {filename}; found: {staged}"
        )

    actual_size = artifact.stat().st_size
    actual_digest = sha256(artifact)
    if actual_size != manifest["sizeBytes"]:
        raise SystemExit(f"Artifact size mismatch: expected {manifest['sizeBytes']}, got {actual_size}")
    if actual_digest != manifest["sha256"]:
        raise SystemExit(f"Artifact digest mismatch: expected {manifest['sha256']}, got {actual_digest}")
    print("Descriptor, manifest, size, SHA-256, and single-artifact policy are consistent.")


def main() -> None:
    parser = argparse.ArgumentParser()
    subparsers = parser.add_subparsers(dest="command", required=True)

    stage_parser = subparsers.add_parser("stage")
    stage_parser.add_argument("artifact", type=Path)
    stage_parser.add_argument("--model-id", required=True)
    stage_parser.add_argument("--version", required=True)
    stage_parser.add_argument("--minimum-runtime-version", default="0.10.35")
    stage_parser.add_argument("--minimum-model-abi", type=int, default=1)

    verify_parser = subparsers.add_parser("verify")
    verify_parser.add_argument("--require-artifact", action="store_true")

    args = parser.parse_args()
    if args.command == "stage":
        stage(args)
    else:
        verify(args.require_artifact)


if __name__ == "__main__":
    main()
