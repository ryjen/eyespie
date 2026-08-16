#!/usr/bin/env python3
"""Validate and compare physical-device image-embedding calibration reports.

Reports are hostile evidence until validated against both the checked-in fixture provenance manifest
and an exact clean candidate-identity manifest produced by release_candidate_identity.py. The tool
evaluates the threshold bound into that candidate; it never selects or rewrites product policy.
"""

from __future__ import annotations

import argparse
import json
import math
from dataclasses import dataclass
from pathlib import Path
from typing import Any

ROOT = Path(__file__).resolve().parents[1]
FIXTURE_MANIFEST = ROOT / "calibration/image-embedding-fixtures.json"
CANDIDATE_IDENTITY_SCHEMA_VERSION = 2
REPORT_SCHEMA_VERSION = 2
REPEAT_COUNT = 5
SEMANTIC_FIXTURES = ("burger_crop", "burger_rotated", "cat")


class CalibrationReportError(ValueError):
    """Raised when calibration evidence is malformed or incompatible."""


@dataclass(frozen=True)
class FixtureSpec:
    fixture_id: str
    role: str
    sha256: str


@dataclass(frozen=True)
class CandidateIdentity:
    candidate: str
    source_commit: str
    application_version: str
    application_build: int
    match_threshold: float
    model_id: str
    model_sha256: str
    embedding_schema_version: int
    dimensions: int
    android_runtime_version: str
    ios_runtime_version: str


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
    application_version: str
    application_build: int
    device: dict[str, str]
    runtime_name: str
    runtime_version: str
    model_id: str
    model_sha256: str
    match_threshold: float
    embedding_schema_version: int
    dimensions: int
    fixtures: dict[str, FixtureResult]


def _read_json(path: Path, description: str) -> dict[str, Any]:
    try:
        payload = json.loads(path.read_text(encoding="utf-8"))
    except (OSError, json.JSONDecodeError) as exc:
        raise CalibrationReportError(f"cannot read {description}: {path}") from exc
    if not isinstance(payload, dict):
        raise CalibrationReportError(f"{description} must be a JSON object")
    return payload


def _nonempty_string(value: Any, field: str) -> str:
    if not isinstance(value, str) or not value.strip():
        raise CalibrationReportError(f"{field} must be a non-empty string")
    return value


def _positive_int(value: Any, field: str) -> int:
    if isinstance(value, bool) or not isinstance(value, int) or value <= 0:
        raise CalibrationReportError(f"{field} must be a positive integer")
    return value


def _finite_number(value: Any, field: str) -> float:
    if isinstance(value, bool) or not isinstance(value, (int, float)):
        raise CalibrationReportError(f"{field} must be numeric")
    result = float(value)
    if not math.isfinite(result):
        raise CalibrationReportError(f"{field} must be finite")
    return result


def _cosine_threshold(value: Any, field: str) -> float:
    result = _finite_number(value, field)
    if result < -1.0 or result > 1.0:
        raise CalibrationReportError(f"{field} must be within [-1, 1]")
    return result


def _lower_sha256(value: Any, field: str) -> str:
    result = _nonempty_string(value, field)
    if len(result) != 64 or any(ch not in "0123456789abcdef" for ch in result):
        raise CalibrationReportError(f"{field} must be 64 lowercase hex characters")
    return result


def _load_fixture_specs() -> dict[str, FixtureSpec]:
    payload = _read_json(FIXTURE_MANIFEST, "calibration fixture manifest")
    if payload.get("schema_version") != 1:
        raise CalibrationReportError("unsupported calibration fixture manifest schema")
    raw_fixtures = payload.get("fixtures")
    if not isinstance(raw_fixtures, list) or not raw_fixtures:
        raise CalibrationReportError("calibration fixture manifest must contain fixtures")

    fixtures: dict[str, FixtureSpec] = {}
    for raw in raw_fixtures:
        if not isinstance(raw, dict):
            raise CalibrationReportError("calibration fixture manifest entries must be objects")
        fixture_id = _nonempty_string(raw.get("id"), "fixture-manifest.id")
        role = _nonempty_string(raw.get("role"), f"fixture-manifest.{fixture_id}.role")
        sha256 = _lower_sha256(raw.get("sha256"), f"fixture-manifest.{fixture_id}.sha256")
        if fixture_id in fixtures:
            raise CalibrationReportError(f"duplicate fixture manifest id: {fixture_id}")
        fixtures[fixture_id] = FixtureSpec(fixture_id, role, sha256)

    expected_ids = ("burger", "burger_crop", "burger_rotated", "cat")
    if tuple(fixtures) != expected_ids:
        raise CalibrationReportError(
            "calibration fixture manifest ids/order must be " + ", ".join(expected_ids)
        )
    return fixtures


FIXTURE_SPECS = _load_fixture_specs()
EXPECTED_FIXTURES = tuple(FIXTURE_SPECS)
EXPECTED_FIXTURE_SHA256 = {
    fixture_id: spec.sha256 for fixture_id, spec in FIXTURE_SPECS.items()
}


def load_candidate_identity(path: Path) -> CandidateIdentity:
    payload = _read_json(path, "candidate identity")
    if payload.get("candidate_identity_schema_version") != CANDIDATE_IDENTITY_SCHEMA_VERSION:
        raise CalibrationReportError(
            "unsupported candidate_identity_schema_version: "
            f"expected {CANDIDATE_IDENTITY_SCHEMA_VERSION}"
        )
    if payload.get("repository") != "ryjen/eyespie":
        raise CalibrationReportError("candidate identity belongs to a different repository")

    source = payload.get("source")
    application = payload.get("application")
    match_policy = payload.get("match_policy")
    embedding = payload.get("image_embedding")
    mediapipe = payload.get("mediapipe")
    if not all(
        isinstance(value, dict)
        for value in (source, application, match_policy, embedding, mediapipe)
    ):
        raise CalibrationReportError(
            "candidate source/application/match_policy/image_embedding/mediapipe objects are required"
        )

    source_commit = _nonempty_string(source.get("commit_sha"), "candidate.source.commit_sha")
    if len(source_commit) != 40 or any(ch not in "0123456789abcdef" for ch in source_commit):
        raise CalibrationReportError("candidate.source.commit_sha must be 40 lowercase hex characters")
    if source.get("dirty") is not False:
        raise CalibrationReportError("candidate identity must come from a clean working tree")

    application_version = _nonempty_string(
        application.get("version"), "candidate.application.version"
    )
    application_build = _positive_int(application.get("build"), "candidate.application.build")
    candidate = _nonempty_string(payload.get("candidate"), "candidate")
    expected_candidate = f"{application_version}+{application_build}@{source_commit[:12]}"
    if candidate != expected_candidate:
        raise CalibrationReportError("candidate identity string does not match source/version/build")

    candidate_threshold = _cosine_threshold(
        match_policy.get("default_cosine_threshold"),
        "candidate.match_policy.default_cosine_threshold",
    )
    model_id = _nonempty_string(embedding.get("model_id"), "candidate.image_embedding.model_id")
    model_sha256 = _lower_sha256(
        embedding.get("model_sha256"), "candidate.image_embedding.model_sha256"
    )
    embedding_schema_version = _positive_int(
        embedding.get("contract_version"), "candidate.image_embedding.contract_version"
    )
    dimensions = _positive_int(embedding.get("dimensions"), "candidate.image_embedding.dimensions")

    android = mediapipe.get("android")
    ios = mediapipe.get("ios")
    if not isinstance(android, dict) or not isinstance(ios, dict):
        raise CalibrationReportError("candidate MediaPipe Android/iOS identities are required")
    android_runtime_version = _nonempty_string(
        android.get("tasks_vision"), "candidate.mediapipe.android.tasks_vision"
    )
    ios_runtime_version = _nonempty_string(
        ios.get("project_artifact_version"), "candidate.mediapipe.ios.project_artifact_version"
    )

    return CandidateIdentity(
        candidate=candidate,
        source_commit=source_commit,
        application_version=application_version,
        application_build=application_build,
        match_threshold=candidate_threshold,
        model_id=model_id,
        model_sha256=model_sha256,
        embedding_schema_version=embedding_schema_version,
        dimensions=dimensions,
        android_runtime_version=android_runtime_version,
        ios_runtime_version=ios_runtime_version,
    )


def load_report(path: Path, candidate: CandidateIdentity) -> CalibrationReport:
    payload = _read_json(path, "calibration report")
    if payload.get("report_schema_version") != REPORT_SCHEMA_VERSION:
        raise CalibrationReportError(
            f"unsupported report_schema_version: expected {REPORT_SCHEMA_VERSION}"
        )

    platform = _nonempty_string(payload.get("platform"), "platform")
    if platform not in {"android", "ios"}:
        raise CalibrationReportError(f"unsupported platform: {platform}")

    application = payload.get("application")
    raw_device = payload.get("device")
    runtime = payload.get("runtime")
    model = payload.get("model")
    match_policy = payload.get("match_policy")
    embedding = payload.get("embedding_contract")
    if not all(
        isinstance(value, dict)
        for value in (application, raw_device, runtime, model, match_policy, embedding)
    ):
        raise CalibrationReportError(
            "application/device/runtime/model/match_policy/embedding_contract objects are required"
        )

    application_version = _nonempty_string(application.get("version"), "application.version")
    application_build = _positive_int(application.get("build"), "application.build")
    if (
        application_version != candidate.application_version
        or application_build != candidate.application_build
    ):
        raise CalibrationReportError("report application identity does not match candidate")

    if not raw_device:
        raise CalibrationReportError("device must be a non-empty object")
    device = {
        str(key): _nonempty_string(value, f"device.{key}")
        for key, value in raw_device.items()
    }

    runtime_name = _nonempty_string(runtime.get("name"), "runtime.name")
    runtime_version = _nonempty_string(runtime.get("version"), "runtime.version")
    expected_runtime_version = (
        candidate.android_runtime_version if platform == "android" else candidate.ios_runtime_version
    )
    if runtime_version != expected_runtime_version:
        raise CalibrationReportError(f"{platform} runtime version does not match candidate identity")

    model_id = _nonempty_string(model.get("id"), "model.id")
    model_sha256 = _lower_sha256(model.get("sha256"), "model.sha256")
    if model_id != candidate.model_id:
        raise CalibrationReportError(
            f"unexpected model.id: expected {candidate.model_id}, got {model_id}"
        )
    if model_sha256 != candidate.model_sha256:
        raise CalibrationReportError(
            "packaged model SHA-256 does not match the candidate image-embedding model"
        )

    match_threshold = _cosine_threshold(
        match_policy.get("cosine_threshold"), "match_policy.cosine_threshold"
    )
    if not math.isclose(match_threshold, candidate.match_threshold, rel_tol=0.0, abs_tol=1e-7):
        raise CalibrationReportError("report match threshold does not match candidate identity")

    schema_version = embedding.get("schema_version")
    dimensions = embedding.get("dimensions")
    if schema_version != candidate.embedding_schema_version:
        raise CalibrationReportError("embedding schema version does not match candidate identity")
    if dimensions != candidate.dimensions:
        raise CalibrationReportError(
            f"embedding dimensions must be {candidate.dimensions}, was {dimensions!r}"
        )

    raw_fixtures = payload.get("fixtures")
    if not isinstance(raw_fixtures, list):
        raise CalibrationReportError("fixtures must be a list")
    fixtures: dict[str, FixtureResult] = {}
    for raw in raw_fixtures:
        if not isinstance(raw, dict):
            raise CalibrationReportError("fixture entries must be objects")
        fixture_id = _nonempty_string(raw.get("id"), "fixture.id")
        role = _nonempty_string(raw.get("role"), f"fixture.{fixture_id}.role")
        spec = FIXTURE_SPECS.get(fixture_id)
        if spec is None or role != spec.role:
            raise CalibrationReportError(f"unexpected fixture identity/role: {fixture_id}/{role}")
        if fixture_id in fixtures:
            raise CalibrationReportError(f"duplicate fixture id: {fixture_id}")

        source_sha256 = _lower_sha256(
            raw.get("source_sha256"), f"fixture.{fixture_id}.source_sha256"
        )
        if source_sha256 != spec.sha256:
            raise CalibrationReportError(
                f"fixture {fixture_id} SHA-256 does not match the pinned calibration fixture"
            )

        raw_values = raw.get("embedding")
        if not isinstance(raw_values, list) or len(raw_values) != candidate.dimensions:
            raise CalibrationReportError(
                f"fixture {fixture_id} embedding must contain exactly {candidate.dimensions} values"
            )
        values = tuple(
            _finite_number(value, f"fixture.{fixture_id}.embedding") for value in raw_values
        )
        if not any(value != 0.0 for value in values):
            raise CalibrationReportError(f"fixture {fixture_id} embedding must have non-zero magnitude")

        repeat_count = raw.get("repeat_count")
        if repeat_count != REPEAT_COUNT:
            raise CalibrationReportError(
                f"fixture {fixture_id} repeat_count must be exactly {REPEAT_COUNT}"
            )
        repeat_cosine_min = _cosine_threshold(
            raw.get("repeat_cosine_min"), f"fixture.{fixture_id}.repeat_cosine_min"
        )
        repeat_max_abs_delta = _finite_number(
            raw.get("repeat_max_abs_delta"), f"fixture.{fixture_id}.repeat_max_abs_delta"
        )
        if repeat_max_abs_delta < 0.0:
            raise CalibrationReportError(f"fixture {fixture_id} repeat delta must be non-negative")

        fixtures[fixture_id] = FixtureResult(
            fixture_id=fixture_id,
            role=role,
            source_sha256=source_sha256,
            embedding=values,
            repeat_count=repeat_count,
            repeat_cosine_min=repeat_cosine_min,
            repeat_max_abs_delta=repeat_max_abs_delta,
        )

    if tuple(fixtures) != EXPECTED_FIXTURES:
        raise CalibrationReportError(f"fixture ids/order must be {', '.join(EXPECTED_FIXTURES)}")

    return CalibrationReport(
        platform=platform,
        application_version=application_version,
        application_build=application_build,
        device=device,
        runtime_name=runtime_name,
        runtime_version=runtime_version,
        model_id=model_id,
        model_sha256=model_sha256,
        match_threshold=match_threshold,
        embedding_schema_version=schema_version,
        dimensions=dimensions,
        fixtures=fixtures,
    )


def cosine_similarity(a: tuple[float, ...], b: tuple[float, ...]) -> float:
    if len(a) != len(b) or not a:
        raise CalibrationReportError("vectors must be non-empty and have equal dimensions")
    dot = sum(x * y for x, y in zip(a, b))
    norm_a = math.sqrt(sum(x * x for x in a))
    norm_b = math.sqrt(sum(y * y for y in b))
    if norm_a == 0.0 or norm_b == 0.0:
        raise CalibrationReportError("vectors must have non-zero magnitude")
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
    return {
        fixture_id: {
            "cosine_similarity": (similarity := cosine_similarity(
                reference, candidate_report.fixtures[fixture_id].embedding
            )),
            "matches_configured_threshold": similarity >= reference_report.match_threshold,
        }
        for fixture_id in SEMANTIC_FIXTURES
    }


def compare_reports(
    android: CalibrationReport,
    ios: CalibrationReport,
    candidate: CandidateIdentity,
) -> dict[str, Any]:
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
            "matches_configured_threshold": similarity >= candidate.match_threshold,
        }

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
        "comparison_schema_version": 2,
        "candidate": {
            "id": candidate.candidate,
            "source_commit": candidate.source_commit,
            "application": {
                "version": candidate.application_version,
                "build": candidate.application_build,
            },
        },
        "model": {"id": android.model_id, "sha256": android.model_sha256},
        "embedding_contract": {
            "schema_version": android.embedding_schema_version,
            "dimensions": android.dimensions,
        },
        "match_policy": {
            "cosine_threshold": candidate.match_threshold,
            "threshold_changed": False,
            "note": (
                "Observed decisions use the threshold bound into the candidate manifest; "
                "calibration does not select a new threshold."
            ),
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
            "all_match": all(
                metrics["matches_configured_threshold"] for metrics in cross_platform.values()
            ),
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
        f"- Candidate: `{comparison['candidate']['id']}`",
        f"- Source commit: `{comparison['candidate']['source_commit']}`",
        f"- Model: `{comparison['model']['id']}`",
        f"- Model SHA-256: `{comparison['model']['sha256']}`",
        f"- Dimensions: {comparison['embedding_contract']['dimensions']}",
        f"- Configured cosine threshold: {comparison['match_policy']['cosine_threshold']:.9g}",
        "- Fixture provenance: **validated against checked-in SHA-256 allowlist**",
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

    lines.extend([
        "",
        "## Within-platform semantic behavior",
        "",
        "| Platform | Fixture | Cosine to reference | Matches configured threshold |",
        "|---|---|---:|---|",
    ])
    for platform in ("android", "ios"):
        for fixture_id, values in comparison[platform]["semantic_behavior"].items():
            decision = "yes" if values["matches_configured_threshold"] else "no"
            lines.append(
                f"| {platform} | {fixture_id} | {values['cosine_similarity']:.9f} | {decision} |"
            )

    lines.extend([
        "",
        "## Cross-platform storage/scan behavior",
        "",
        "| Stored reference | Scan platform | Fixture | Cosine | Matches configured threshold |",
        "|---|---|---|---:|---|",
    ])
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

    lines.extend([
        "",
        "## Repeated-inference stability",
        "",
        "| Platform | Fixture | Runs | Min cosine to first | Max abs delta |",
        "|---|---|---:|---:|---:|",
    ])
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
    validate.add_argument("--candidate-identity", type=Path, required=True)

    compare = subparsers.add_parser("compare", help="compare Android and iOS device reports")
    compare.add_argument("android_report", type=Path)
    compare.add_argument("ios_report", type=Path)
    compare.add_argument("--candidate-identity", type=Path, required=True)
    compare.add_argument("--json-output", type=Path)
    compare.add_argument("--markdown-output", type=Path)

    args = parser.parse_args()
    try:
        candidate = load_candidate_identity(args.candidate_identity)
        if args.command == "validate":
            report = load_report(args.report, candidate)
            print(
                f"validated {report.platform} calibration report: "
                f"candidate={candidate.candidate}; "
                f"{report.runtime_name} {report.runtime_version}; "
                f"model={report.model_sha256}; threshold={report.match_threshold:.9g}"
            )
        else:
            android = load_report(args.android_report, candidate)
            ios = load_report(args.ios_report, candidate)
            result = compare_reports(android, ios, candidate)
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
