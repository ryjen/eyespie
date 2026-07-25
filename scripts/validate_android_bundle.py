#!/usr/bin/env python3
"""Validate the packaged Android App Bundle module and model-pack topology."""

from __future__ import annotations

import argparse
import html
import sys
import xml.etree.ElementTree as ET
import zipfile
from dataclasses import dataclass
from pathlib import Path, PurePosixPath

BASE_MODULE = "base"
MODEL_PACK_MODULE = "model_pack"
MODEL_MANIFEST = f"{MODEL_PACK_MODULE}/assets/model/manifest.json"
BASE_MODEL_PREFIX = f"{BASE_MODULE}/assets/model/"
DIST_NAMESPACE = "http://schemas.android.com/apk/distribution"
DELIVERY_MODES = {"install-time", "fast-follow", "on-demand"}


class ValidationError(RuntimeError):
    """Raised when an App Bundle violates the expected topology."""


@dataclass(frozen=True)
class ModuleSize:
    compressed: int = 0
    uncompressed: int = 0
    files: int = 0


def module_for_entry(name: str) -> str | None:
    path = PurePosixPath(name)
    if not path.parts or name.endswith("/"):
        return None
    return path.parts[0]


def inventory(archive: zipfile.ZipFile) -> tuple[set[str], dict[str, ModuleSize]]:
    modules: set[str] = set()
    sizes: dict[str, ModuleSize] = {}
    for entry in archive.infolist():
        module = module_for_entry(entry.filename)
        if module is None:
            continue
        modules.add(module)
        current = sizes.get(module, ModuleSize())
        sizes[module] = ModuleSize(
            compressed=current.compressed + entry.compress_size,
            uncompressed=current.uncompressed + entry.file_size,
            files=current.files + 1,
        )
    return modules, sizes


def parse_asset_pack_delivery_mode(manifest_xml: str) -> str:
    try:
        root = ET.fromstring(manifest_xml)
    except ET.ParseError as error:
        raise ValidationError(f"invalid decoded model_pack manifest XML: {error}") from error

    module = root.find(f"{{{DIST_NAMESPACE}}}module")
    if module is None:
        raise ValidationError("model_pack manifest is missing dist:module")

    module_type = module.get(f"{{{DIST_NAMESPACE}}}type")
    if module_type != "asset-pack":
        raise ValidationError(
            f"model_pack dist:module type must be asset-pack, found {module_type!r}"
        )

    delivery = module.find(f"{{{DIST_NAMESPACE}}}delivery")
    if delivery is None:
        raise ValidationError("model_pack manifest is missing dist:delivery")

    modes = [
        child.tag.rsplit("}", 1)[-1]
        for child in delivery
        if child.tag.startswith(f"{{{DIST_NAMESPACE}}}")
        and child.tag.rsplit("}", 1)[-1] in DELIVERY_MODES
    ]
    if len(modes) != 1:
        raise ValidationError(
            "model_pack dist:delivery must contain exactly one delivery mode; "
            f"found {modes or 'none'}"
        )
    return modes[0]


def validate_inventory(entries: set[str], manifest_xml: str) -> None:
    modules = {
        PurePosixPath(entry).parts[0]
        for entry in entries
        if PurePosixPath(entry).parts
    }
    if BASE_MODULE not in modules:
        raise ValidationError("base module is absent from the App Bundle")
    if MODEL_PACK_MODULE not in modules:
        raise ValidationError("model_pack is absent from the App Bundle")
    if MODEL_MANIFEST not in entries:
        raise ValidationError(f"required model manifest is absent: {MODEL_MANIFEST}")

    leaked = sorted(
        entry
        for entry in entries
        if entry.startswith(BASE_MODEL_PREFIX)
        or (entry.startswith(f"{BASE_MODULE}/") and entry.endswith("/model/manifest.json"))
        or (entry.startswith(f"{BASE_MODULE}/") and entry.lower().endswith(".task"))
    )
    if leaked:
        raise ValidationError(
            "model-pack assets leaked into the base module: " + ", ".join(leaked)
        )

    delivery_mode = parse_asset_pack_delivery_mode(manifest_xml)
    if delivery_mode != "on-demand":
        raise ValidationError(
            f"model_pack delivery mode must be on-demand, found {delivery_mode}"
        )


def human_bytes(value: int) -> str:
    units = ("B", "KiB", "MiB", "GiB")
    number = float(value)
    for unit in units:
        if number < 1024 or unit == units[-1]:
            return f"{number:.1f} {unit}" if unit != "B" else f"{int(number)} B"
        number /= 1024
    raise AssertionError("unreachable")


def render_report(aab: Path, sizes: dict[str, ModuleSize]) -> str:
    rows = []
    for module in sorted(sizes):
        size = sizes[module]
        rows.append(
            f"| `{html.escape(module)}` | {size.files} | {human_bytes(size.compressed)} | "
            f"{human_bytes(size.uncompressed)} |"
        )
    return "\n".join(
        [
            "# Android App Bundle topology",
            "",
            f"- Bundle: `{html.escape(aab.name)}`",
            f"- Total archive size: {human_bytes(aab.stat().st_size)}",
            f"- Required modules: `{BASE_MODULE}`, `{MODEL_PACK_MODULE}`",
            f"- Required model manifest: `{MODEL_MANIFEST}`",
            "- Delivery mode: `model_pack` is structurally configured as `dist:on-demand`",
            "",
            "| Module | Files | Compressed | Uncompressed |",
            "|---|---:|---:|---:|",
            *rows,
            "",
        ]
    )


def validate_bundle(aab: Path, manifest_path: Path, report_path: Path) -> None:
    if not aab.is_file():
        raise ValidationError(f"App Bundle does not exist: {aab}")
    if not manifest_path.is_file():
        raise ValidationError(f"decoded model_pack manifest does not exist: {manifest_path}")

    manifest_xml = manifest_path.read_text(encoding="utf-8")
    with zipfile.ZipFile(aab) as archive:
        entries = {entry.filename for entry in archive.infolist() if not entry.is_dir()}
        _, sizes = inventory(archive)
    validate_inventory(entries, manifest_xml)

    report_path.parent.mkdir(parents=True, exist_ok=True)
    report_path.write_text(render_report(aab, sizes), encoding="utf-8")
    print(report_path.read_text(encoding="utf-8"), end="")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--aab", required=True, type=Path)
    parser.add_argument("--model-pack-manifest", required=True, type=Path)
    parser.add_argument("--report", required=True, type=Path)
    args = parser.parse_args()

    try:
        validate_bundle(args.aab, args.model_pack_manifest, args.report)
    except (ValidationError, zipfile.BadZipFile, OSError) as error:
        print(f"Android bundle validation failed: {error}", file=sys.stderr)
        return 1
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
