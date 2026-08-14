#!/usr/bin/env python3
"""Validate and compare physical-device image-embedding calibration reports.

This tool evaluates observed behavior against the match policy recorded by the device builds.
It never chooses or rewrites that policy: schema, normalization, and threshold changes remain
separate reviewed product decisions.
"""

from __future__ import annotations

import argparse
import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Any

DIMENSIONS = 1024
EXPECTED_MODEL_ID = "mobilenet-v3-small-100-224-embedder"
EXPECTED_MODEL_SHA256 = "f7b9a563cb803bdcba76e8c7e82abde06f5c7a8e67b5e54e43e23095dfe79a78"
EXPECTED_FIXTURES = ("burger", "burger_crop", "burger_rotated", "cat")
EXPECTED_ROLES = {
    "burger": "reference",
    "burger_crop": "related",
    "burger_rotated": "related",
    "cat": "unrelated",
}
EXPECTED_FIXTURE_SHA256 = {
    "burger": "97c15bbbf3cf3615063b1031c85d669de55839f59262bbe145d15ca75b36ecbf",
    "burger_crop": "8f58de573f0bf59a49c3d86cfabb9ad4061481f574aa049177e8da3963dddc50",
    "burger_rotated": "b7bb5e59ef778f3ce6b3e616c511908a53d513b83a56aae58b7453e14b0a4b2a",
    "cat": "2533197401eebe9410ea4d063f86c43fbd2666f3e8165a38aca155c0d09c21be",
}
SEMANTIC_FIXTURES = ("burger_crop", "burger_rotated", "cat")


class CalibrationReportError(ValueError):
    """Raised when calibration evidence is malformed or incompatible."""


@dataclass(frozen=True)
class FixtureResult:
    fixture_id: str
    role: str
    source_sha256: str
    embedding: tuple[float, ...]
    repeat_count: int
    repeat_cosine_min: float
    repeat_max_abs_delta: float


@dataclass(frozen=True)
class CalibrationReport:
    platform: str
    device: dict[str, str]
    runtime_name: str
    runtime_version: str
    model_id: str
    model_sha256: str
    match_threshold: float
    embedding_schema_version: int
    dimensions: int
    fixtures: dict[str, FixtureResult]


def _finite_number(value: Any, field: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise CalibrationReportError(f"{field} must be numeric")
    result = float(value)
    if not math.isfinite(result):
        raise CalibrationReportError(f"{field} must be finite")
    return result


def _nonempty_string(value: Any, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise CalibrationReportError(f"{field} must be a non-empty string")
    return value


def load_report(path: Path) -> CalibrationReport:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise CalibrationReportError(f"cannot read calibration report: {path}") from exc

    if payload.get("report_schema_version") != 1:
        raise CalibrationReportError("unsupported report_schema_version")
    platform = _nonempty_string(payload.get("platform"), "platform")
    if platform not in {"android", "ios"}:
        raise CalibrationReportError(f"unsupported platform: {platform}")

    raw_device = payload.get("device")
    if not isinstance(raw_device, dict) or not raw_device:
        raise CalibrationReportError("device must be a non-empty object")
    device = {str(key): _nonempty_string(value, f"device.{key}") for key, value in raw_device.items()}

    runtime = payload.get("runtime")
    model = payload.get("model")
    match_policy = payload.get("match_policy")
    embedding = payload.get("embedding_contract")
    if (
        not isinstance(runtime, dict)
        or not isinstance(model, dict)
        or not isinstance(match_policy, dict)
        or not isinstance(embedding, dict)
    ):
        raise CalibrationReportError(
            "runtime/model/match_policy/embedding_contract objects are required"
        )

    runtime_name = _nonempty_string(runtime.get("name"), "runtime.name")
    runtime_version = _nonempty_string(runtime.get("version"), "runtime.version")
    model_id = _nonempty_string(model.get("id"), "model.id")
    model_sha256 = _nonempty_string(model.get("sha256"), "model.sha256")
    if len(model_sha256) != 64 or any(ch not in "0123456789abcdef" for ch in model_sha256):
        raise CalibrationReportError("model.sha256 must be 64 lowercase hex characters")
    if model_id != EXPECTED_MODEL_ID:
        raise CalibrationReportError(
            f"unexpected model.id: expected {EXPECTED_MODEL_ID}, got {model_id}"
        )
    if model_sha256 != EXPECTED_MODEL_SHA256:
        raise CalibrationReportError(
            "packaged model SHA-256 does not match the pinned image-embedding model"
        )

    match_threshold = _finite_number(
        match_policy.get("cosine_threshold"), "match_policy.cosine_threshold"
    )
    if match_threshold < -1.0 or match_threshold > 1.0:
        raise CalibrationReportError("match_policy.cosine_threshold must be within [-1, 1]")

    schema_version = embedding.get("schema_version")
    dimensions = embedding.get("dimensions")
    if schema_version != 1:
        raise CalibrationReportError(f"unexpected embedding schema version: {schema_version!r}")
    if dimensions != DIMENSIONS:
        raise CalibrationReportError(f"embedding dimensions must be {DIMENSIONS}, was {dimensions!r}")

    raw_fixtures = payload.get("fixtures")
    if not isinstance(raw_fixtures, list):
        raise CalibrationReportError("fixtures must be a list")
    fixtures: dict[str, FixtureResult] = {}
    for raw in raw_fixtures:
        if not isinstance(raw, dict):
            raise CalibrationReportError("fixture entries must be objects")
        fixture_id = _nonempty_string(raw.get("id"), "fixture.id")
        role = _nonempty_string(raw.get("role"), f"fixture.{fixture_id}.role")
        if fixture_id not in EXPECTED_ROLES or role != EXPECTED_ROLES[fixture_id]:
            raise CalibrationReportError(f"unexpected fixture identity/role: {fixture_id}/{role}")
        if fixture_id in fixtures:
            raise CalibrationReportError(f"duplicate fixture id: {fixture_id}")

        source_sha256 = _nonempty_string(
            raw.get("source_sha256"), f"fixture.{fixture_id}.source_sha256"
        )
        if source_sha256 != EXPECTED_FIXTURE_SHA256[fixture_id]:
            raise CalibrationReportError(
                f"fixture {fixture_id} SHA-256 does not match the pinned calibration fixture"
            )

        raw_values = raw.get("embedding")
        if not isinstance(raw_values, list) or len(raw_values) != DIMENSIONS:
            raise CalibrationReportError(
                f"fixture {fixture_id} embedding must contain exactly {DIMENSIONS} values"
            )
        values = tuple(_finite_number(value, f"fixture.{fixture_id}.embedding") for value in raw_values)

        repeat_count = raw.get("repeat_count")
        if isinstance(repeat_count, bool) or not isinstance(repeat_count, int) or repeat_count < 2:
            raise CalibrationReportError(f"fixture {fixture_id} repeat_count must be >= 2")
        repeat_cosine_min = _finite_number(
            raw.get("repeat_cosine_min"), f"fixture.{fixture_id}.repeat_cosine_min"
        )
        repeat_max_abs_delta = _finite_number(
            raw.get("repeat_max_abs_delta"), f"fixture.{fixture_id}.repeat_max_abs_delta"
        )
        if repeat_cosine_min < -1.0 or repeat_cosine_min > 1.0:
            raise CalibrationReportError(f"fixture {fixture_id} repeat cosine is outside [-1, 1]")
        if repeat_max_abs_delta < 0.0:
            raise CalibrationReportError(f"fixture {fixture_id} repeat delta must be non-negative")

        fixtures[fixture_id] = FixtureResult(
            fixture_id,
            role,
            source_sha256,
            values,
            repeat_count,
            repeat_cosine_min,
            repeat_max_abs_delta,
        )

    if tuple(fixtures) != EXPECTED_FIXTURES:
        raise CalibrationReportError(
            f"fixture ids/order must be {', '.join(EXPECTED_FIXTURES)}"
        )

    return CalibrationReport(
        platform,
        device,
        runtime_name,
        runtime_version,
        model_id,
        model_sha256,
        match_threshold,
        schema_version,
        dimensions,
        fixtures,
    )


def cosine_similarity(a: tuple[float, ...], b: tuple[float, ...]) -> float:
    if len(a) != len(b) or not a:
        raise CalibrationReportError("vectors must be non-empty and have equal dimensions")
    dot = sum(x * y for x, y in zip(a, b))
    norm_a = math.sqrt(sum(x * x for x in a))
    norm_b = math.sqrt(sum(y * y for y in b))
    if norm_a == 0.0 or norm_b == 0.0:
        return 0.0
    return dot / (norm_a * norm_b)


def rmse(a: tuple[float, ...], b: tuple[float, ...]) -> float:
    if len(a) != len(b) or not a:
        raise CalibrationReportError("vectors must be non-empty and have equal dimensions")
    return math.sqrt(sum((x - y) ** 2 for x, y in zip(a, b)) / len(a))


def max_abs_delta(a: tuple[float, ...], b: tuple[float, ...]) -> float:
    if len(a) != len(b) or not a:
        raise CalibrationReportError("vectors must be non-empty and have equal dimensions")
    return max(abs(x - y) for x, y in zip(a, b))


def _semantic_metrics(
    reference_report: CalibrationReport,
    candidate_report: CalibrationReport,
) -> dict[str, dict[str, Any]]:
    reference = reference_report.fixtures["burger"].embedding
    result: dict[str, dict[str, Any]] = {}
    for fixture_id in SEMANTIC_FIXTURES:
        similarity = cosine_similarity(reference, candidate_report.fixtures[fixture_id].embedding)
        result[fixture_id] = {
            "cosine_similarity": similarity,
            "matches_configured_threshold": similarity >= reference_report.match_threshold,
        }
    return result


def compare_reports(android: CalibrationReport, ios: CalibrationReport) -> dict[str, Any]:
    if android.platform != "android" or ios.platform != "ios":
        raise CalibrationReportError("compare requires Android report first and iOS report second")
    if android.model_id != ios.model_id or android.model_sha256 != ios.model_sha256:
        raise CalibrationReportError("reports use different model identities")
    if (
        android.embedding_schema_version != ios.embedding_schema_version
        or android.dimensions != ios.dimensions
    ):
        raise CalibrationReportError("reports use different embedding contracts")
    if not math.isclose(android.match_threshold, ios.match_threshold, rel_tol=0.0, abs_tol=1e-7):
        raise CalibrationReportError("reports use different configured match thresholds")

    android_semantic = _semantic_metrics(android, android)
    ios_semantic = _semantic_metrics(ios, ios)
    android_reference_to_ios = _semantic_metrics(android, ios)
    ios_reference_to_android = _semantic_metrics(ios, android)

    cross_platform: dict[str, dict[str, Any]] = {}
    for fixture_id in EXPECTED_FIXTURES:
        a = android.fixtures[fixture_id].embedding
        b = ios.fixtures[fixture_id].embedding
        similarity = cosine_similarity(a, b)
        cross_platform[fixture_id] = {
            "cosine_similarity": similarity,
            "rmse": rmse(a, b),
            "max_abs_delta": max_abs_delta(a, b),
            "matches_configured_threshold": similarity >= android.match_threshold,
        }
    same_fixture_all_match = all(
        metrics["matches_configured_threshold"] for metrics in cross_platform.values()
    )

    decision_sets = {
        fixture_id: (
            android_semantic[fixture_id]["matches_configured_threshold"],
            ios_semantic[fixture_id]["matches_configured_threshold"],
            android_reference_to_ios[fixture_id]["matches_configured_threshold"],
            ios_reference_to_android[fixture_id]["matches_configured_threshold"],
        )
        for fixture_id in SEMANTIC_FIXTURES
    }
    decision_consistency = {
        fixture_id: len(set(decisions)) == 1
        for fixture_id, decisions in decision_sets.items()
    }

    return {
        "comparison_schema_version": 1,
        "model": {"id": android.model_id, "sha256": android.model_sha256},
        "embedding_contract": {
            "schema_version": android.embedding_schema_version,
            "dimensions": android.dimensions,
        },
        "match_policy": {
            "cosine_threshold": android.match_threshold,
            "threshold_changed": False,
            "note": "Observed decisions use the threshold recorded by both builds; calibration does not select a new threshold.",
        },
        "fixtures": {
            fixture_id: {"sha256": EXPECTED_FIXTURE_SHA256[fixture_id]}
            for fixture_id in EXPECTED_FIXTURES
        },
        "android": {
            "runtime": {"name": android.runtime_name, "version": android.runtime_version},
            "device": android.device,
            "semantic_behavior": android_semantic,
            "repeat_stability": {
                fixture_id: {
                    "repeat_count": result.repeat_count,
                    "cosine_min": result.repeat_cosine_min,
                    "max_abs_delta": result.repeat_max_abs_delta,
                }
                for fixture_id, result in android.fixtures.items()
            },
        },
        "ios": {
            "runtime": {"name": ios.runtime_name, "version": ios.runtime_version},
            "device": ios.device,
            "semantic_behavior": ios_semantic,
            "repeat_stability": {
                fixture_id: {
                    "repeat_count": result.repeat_count,
                    "cosine_min": result.repeat_cosine_min,
                    "max_abs_delta": result.repeat_max_abs_delta,
                }
                for fixture_id, result in ios.fixtures.items()
            },
        },
        "cross_platform": cross_platform,
        "same_fixture_cross_platform_match": {
            "all_match": same_fixture_all_match,
            "fixtures": {
                fixture_id: metrics["matches_configured_threshold"]
                for fixture_id, metrics in cross_platform.items()
            },
        },
        "cross_platform_semantic_behavior": {
            "android_reference_to_ios": android_reference_to_ios,
            "ios_reference_to_android": ios_reference_to_android,
        },
        "match_decision_consistency": {
            "fixtures": decision_consistency,
            "all_consistent": all(decision_consistency.values()),
            "contexts": [
                "android_reference_to_android",
                "ios_reference_to_ios",
                "android_reference_to_ios",
                "ios_reference_to_android",
            ],
        },
    }


def require_same_fixture_cross_platform_match(comparison: dict[str, Any]) -> None:
    fixture_results = comparison["same_fixture_cross_platform_match"]["fixtures"]
    failed = [fixture_id for fixture_id, matched in fixture_results.items() if not matched]
    if failed:
        raise CalibrationReportError(
            "exact same-fixture cross-platform matching failed at the configured threshold: "
            + ", ".join(failed)
        )


def render_markdown(comparison: dict[str, Any]) -> str:
    lines = [
        "# Image embedding calibration comparison",
        "",
        f"- Model: `{comparison['model']['id']}`",
        f"- Model SHA-256: `{comparison['model']['sha256']}`",
        f"- Dimensions: {comparison['embedding_contract']['dimensions']}",
        f"- Configured cosine threshold: {comparison['match_policy']['cosine_threshold']:.9g}",
        "- Fixture provenance: **validated against pinned SHA-256 values**",
        "- Product match threshold changed: **no**",
        f"- Exact same fixtures match across platforms: **{'yes' if comparison['same_fixture_cross_platform_match']['all_match'] else 'no'}**",
        f"- Match decisions consistent across both storage/scan directions: **{'yes' if comparison['match_decision_consistency']['all_consistent'] else 'no'}**",
        "",
        "## Cross-platform same-fixture metrics",
        "",
        "| Fixture | Cosine | RMSE | Max abs delta | Matches configured threshold |",
        "|---|---:|---:|---:|---|",
    ]
    for fixture_id, metrics in comparison["cross_platform"].items():
        decision = "yes" if metrics["matches_configured_threshold"] else "no"
        lines.append(
            f"| {fixture_id} | {metrics['cosine_similarity']:.9f} | "
            f"{metrics['rmse']:.9g} | {metrics['max_abs_delta']:.9g} | {decision} |"
        )

    lines.extend(
        [
            "",
            "## Within-platform semantic behavior",
            "",
            "| Platform | Fixture | Cosine to reference | Matches configured threshold |",
            "|---|---|---:|---|",
        ]
    )
    for platform in ("android", "ios"):
        for fixture_id, values in comparison[platform]["semantic_behavior"].items():
            decision = "yes" if values["matches_configured_threshold"] else "no"
            lines.append(
                f"| {platform} | {fixture_id} | {values['cosine_similarity']:.9f} | {decision} |"
            )

    lines.extend(
        [
            "",
            "## Cross-platform storage/scan behavior",
            "",
            "| Stored reference | Scan platform | Fixture | Cosine | Matches configured threshold |",
            "|---|---|---|---:|---|",
        ]
    )
    for direction, stored_platform, scan_platform in (
        ("android_reference_to_ios", "android", "ios"),
        ("ios_reference_to_android", "ios", "android"),
    ):
        for fixture_id, values in comparison["cross_platform_semantic_behavior"][direction].items():
            decision = "yes" if values["matches_configured_threshold"] else "no"
            lines.append(
                f"| {stored_platform} | {scan_platform} | {fixture_id} | "
                f"{values['cosine_similarity']:.9f} | {decision} |"
            )

    lines.extend(
        [
            "",
            "## Match-decision consistency",
            "",
            "Consistency requires the same decision for Android→Android, iOS→iOS, Android→iOS, and iOS→Android.",
            "",
            "| Fixture | Consistent |",
            "|---|---|",
        ]
    )
    for fixture_id, consistent in comparison["match_decision_consistency"]["fixtures"].items():
        lines.append(f"| {fixture_id} | {'yes' if consistent else 'no'} |")

    lines.extend(
        [
            "",
            "## Repeated-inference stability",
            "",
            "| Platform | Fixture | Runs | Min cosine to first | Max abs delta |",
            "|---|---|---:|---:|---:|",
        ]
    )
    for platform in ("android", "ios"):
        for fixture_id, values in comparison[platform]["repeat_stability"].items():
            lines.append(
                f"| {platform} | {fixture_id} | {values['repeat_count']} | "
                f"{values['cosine_min']:.9f} | {values['max_abs_delta']:.9g} |"
            )
    return "\n".join(lines) + "\n"


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    subparsers = parser.add_subparsers(dest="command", required=True)

    validate = subparsers.add_parser("validate", help="validate one device report")
    validate.add_argument("report", type=Path)

    compare = subparsers.add_parser("compare", help="compare Android and iOS device reports")
    compare.add_argument("android_report", type=Path)
    compare.add_argument("ios_report", type=Path)
    compare.add_argument("--json-output", type=Path)
    compare.add_argument("--markdown-output", type=Path)

    args = parser.parse_args()
    try:
        if args.command == "validate":
            report = load_report(args.report)
            print(
                f"validated {report.platform} calibration report: "
                f"{report.runtime_name} {report.runtime_version}; "
                f"model={report.model_sha256}; threshold={report.match_threshold:.9g}"
            )
        else:
            android = load_report(args.android_report)
            ios = load_report(args.ios_report)
            result = compare_reports(android, ios)
            encoded = json.dumps(result, indent=2, sort_keys=True) + "\n"
            markdown = render_markdown(result)
            if args.json_output:
                args.json_output.write_text(encoded, encoding="utf-8")
            if args.markdown_output:
                args.markdown_output.write_text(markdown, encoding="utf-8")
            if not args.json_output and not args.markdown_output:
                print(markdown, end="")
            require_same_fixture_cross_platform_match(result)
    except CalibrationReportError as exc:
        parser.error(str(exc))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
