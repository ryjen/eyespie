from __future__ import annotations

import json
import math
import tempfile
import unittest
from pathlib import Path

from scripts.compare_image_embedding_calibration import (
    EXPECTED_FIXTURE_SHA256,
    CalibrationReportError,
    compare_reports,
    load_candidate_identity,
    load_report,
    require_same_fixture_cross_platform_match,
)
from scripts.release_candidate_identity import build_identity


class ImageEmbeddingCalibrationComparatorTest(unittest.TestCase):
    def vector(self, *components: float) -> list[float]:
        return list(components) + [0.0] * (1024 - len(components))

    def write_candidate(self, root: Path) -> Path:
        path = root / "candidate.json"
        payload = build_identity(allow_dirty=True)
        # Unit tests use synthetic evidence; local uncommitted work must not alter these fixtures.
        payload["source"]["dirty"] = False
        path.write_text(json.dumps(payload), encoding="utf-8")
        return path

    def write_report(
        self,
        root: Path,
        platform: str,
        candidate_path: Path,
        *,
        model_id: str | None = None,
        model_sha256: str | None = None,
        application_version: str | None = None,
        application_build: int | None = None,
        runtime_version: str | None = None,
        report_schema_version: int = 2,
        match_threshold: float = 0.5,
        repeat_count: int = 5,
        non_finite: bool = False,
        reference: tuple[float, float] = (1.0, 0.0),
        crop: tuple[float, float] = (0.98, 0.02),
        rotated: tuple[float, float] = (0.95, 0.05),
        cat: tuple[float, float] = (0.0, 1.0),
    ) -> Path:
        candidate_payload = json.loads(candidate_path.read_text(encoding="utf-8"))
        expected_runtime = (
            candidate_payload["mediapipe"]["android"]["tasks_vision"]
            if platform == "android"
            else candidate_payload["mediapipe"]["ios"]["project_artifact_version"]
        )
        reference_vector = self.vector(*reference)
        crop_vector = self.vector(*crop)
        rotated_vector = self.vector(*rotated)
        cat_vector = self.vector(*cat)
        if non_finite:
            reference_vector[5] = math.nan
        fixtures = [
            ("burger", "reference", reference_vector),
            ("burger_crop", "related", crop_vector),
            ("burger_rotated", "related", rotated_vector),
            ("cat", "unrelated", cat_vector),
        ]
        payload = {
            "report_schema_version": report_schema_version,
            "platform": platform,
            "application": {
                "version": application_version
                or candidate_payload["application"]["version"],
                "build": application_build
                or candidate_payload["application"]["build"],
            },
            "device": {"manufacturer": "test", "model": platform, "os": "test-os"},
            "runtime": {
                "name": f"runtime-{platform}",
                "version": runtime_version or expected_runtime,
            },
            "model": {
                "id": model_id or candidate_payload["image_embedding"]["model_id"],
                "sha256": model_sha256
                or candidate_payload["image_embedding"]["model_sha256"],
            },
            "match_policy": {"cosine_threshold": match_threshold},
            "embedding_contract": {
                "schema_version": candidate_payload["image_embedding"]["contract_version"],
                "dimensions": candidate_payload["image_embedding"]["dimensions"],
            },
            "fixtures": [
                {
                    "id": fixture_id,
                    "role": role,
                    "source_sha256": EXPECTED_FIXTURE_SHA256[fixture_id],
                    "embedding": vector,
                    "repeat_count": repeat_count,
                    "repeat_cosine_min": 1.0,
                    "repeat_max_abs_delta": 0.0,
                }
                for fixture_id, role, vector in fixtures
            ],
        }
        path = root / f"{platform}.json"
        path.write_text(json.dumps(payload), encoding="utf-8")
        return path

    def load_pair(self, root: Path, **report_kwargs):
        candidate_path = self.write_candidate(root)
        candidate = load_candidate_identity(candidate_path)
        android = load_report(
            self.write_report(root, "android", candidate_path, **report_kwargs),
            candidate,
        )
        ios = load_report(
            self.write_report(root, "ios", candidate_path, **report_kwargs),
            candidate,
        )
        return candidate, android, ios

    def test_compares_observed_behavior_without_changing_threshold(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate, android, ios = self.load_pair(root)

            result = compare_reports(android, ios, candidate)

            self.assertEqual(candidate.candidate, result["candidate"]["id"])
            self.assertEqual(candidate.source_commit, result["candidate"]["source_commit"])
            self.assertEqual(1.0, result["cross_platform"]["burger"]["cosine_similarity"])
            self.assertEqual(0.0, result["cross_platform"]["burger"]["rmse"])
            self.assertTrue(result["same_fixture_cross_platform_match"]["all_match"])
            self.assertGreater(
                result["android"]["semantic_behavior"]["burger_crop"]["cosine_similarity"],
                result["android"]["semantic_behavior"]["cat"]["cosine_similarity"],
            )
            self.assertTrue(result["match_decision_consistency"]["all_consistent"])
            self.assertFalse(result["match_policy"]["threshold_changed"])
            self.assertEqual(0.5, result["match_policy"]["cosine_threshold"])
            require_same_fixture_cross_platform_match(result)

    def test_reports_inconsistent_within_platform_match_decision(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            android = load_report(
                self.write_report(
                    root,
                    "android",
                    candidate_path,
                    crop=(0.8, 0.6),
                ),
                candidate,
            )
            ios = load_report(
                self.write_report(
                    root,
                    "ios",
                    candidate_path,
                    crop=(0.4, 0.916515138991168),
                ),
                candidate,
            )

            result = compare_reports(android, ios, candidate)

            self.assertFalse(
                result["match_decision_consistency"]["fixtures"]["burger_crop"]
            )
            self.assertFalse(result["match_decision_consistency"]["all_consistent"])

    def test_detects_basis_shift_that_only_breaks_cross_platform_matching(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            android = load_report(
                self.write_report(
                    root,
                    "android",
                    candidate_path,
                    match_threshold=0.8,
                    reference=(1.0, 0.0),
                    crop=(1.0, 0.0),
                    rotated=(1.0, 0.0),
                ),
                candidate,
            )
            ios = load_report(
                self.write_report(
                    root,
                    "ios",
                    candidate_path,
                    match_threshold=0.8,
                    reference=(0.7, 0.714142842),
                    crop=(0.7, 0.714142842),
                    rotated=(0.7, 0.714142842),
                ),
                candidate,
            )

            result = compare_reports(android, ios, candidate)

            self.assertFalse(result["same_fixture_cross_platform_match"]["all_match"])
            self.assertFalse(result["match_decision_consistency"]["all_consistent"])
            with self.assertRaisesRegex(
                CalibrationReportError,
                "same-fixture cross-platform matching failed.*burger",
            ):
                require_same_fixture_cross_platform_match(result)

    def test_rejects_historical_model_id(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            path = self.write_report(
                root,
                "android",
                candidate_path,
                model_id="mobilenet-v3-small-100-224-embedder",
            )

            with self.assertRaisesRegex(CalibrationReportError, "unexpected model.id"):
                load_report(path, candidate)

    def test_rejects_unpinned_packaged_model(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            path = self.write_report(
                root,
                "android",
                candidate_path,
                model_sha256="b" * 64,
            )

            with self.assertRaisesRegex(CalibrationReportError, "candidate image-embedding model"):
                load_report(path, candidate)

    def test_rejects_candidate_application_mismatch(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            path = self.write_report(
                root,
                "android",
                candidate_path,
                application_version="9.9.9",
            )

            with self.assertRaisesRegex(CalibrationReportError, "application identity"):
                load_report(path, candidate)

    def test_rejects_runtime_version_mismatch(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            path = self.write_report(
                root,
                "ios",
                candidate_path,
                runtime_version="0.0.0",
            )

            with self.assertRaisesRegex(CalibrationReportError, "runtime version"):
                load_report(path, candidate)

    def test_rejects_historical_report_schema(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            path = self.write_report(
                root,
                "android",
                candidate_path,
                report_schema_version=1,
            )

            with self.assertRaisesRegex(CalibrationReportError, "report_schema_version"):
                load_report(path, candidate)

    def test_rejects_unpinned_fixture_bytes(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            path = self.write_report(root, "android", candidate_path)
            data = json.loads(path.read_text(encoding="utf-8"))
            data["fixtures"][0]["source_sha256"] = "b" * 64
            path.write_text(json.dumps(data), encoding="utf-8")

            with self.assertRaisesRegex(CalibrationReportError, "pinned calibration fixture"):
                load_report(path, candidate)

    def test_rejects_different_configured_thresholds(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            android = load_report(
                self.write_report(root, "android", candidate_path, match_threshold=0.5),
                candidate,
            )
            ios = load_report(
                self.write_report(root, "ios", candidate_path, match_threshold=0.6),
                candidate,
            )

            with self.assertRaisesRegex(CalibrationReportError, "different configured match thresholds"):
                compare_reports(android, ios, candidate)

    def test_rejects_non_finite_embedding_value(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            path = self.write_report(root, "android", candidate_path, non_finite=True)

            with self.assertRaisesRegex(CalibrationReportError, "must be finite"):
                load_report(path, candidate)

    def test_rejects_wrong_dimension(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            path = self.write_report(root, "android", candidate_path)
            data = json.loads(path.read_text(encoding="utf-8"))
            data["fixtures"][0]["embedding"] = data["fixtures"][0]["embedding"][:-1]
            path.write_text(json.dumps(data), encoding="utf-8")

            with self.assertRaisesRegex(CalibrationReportError, "exactly 1024"):
                load_report(path, candidate)

    def test_rejects_non_bounded_repeat_count(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            candidate = load_candidate_identity(candidate_path)
            path = self.write_report(root, "android", candidate_path, repeat_count=6)

            with self.assertRaisesRegex(CalibrationReportError, "exactly 5"):
                load_report(path, candidate)

    def test_rejects_dirty_candidate_identity(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate_path = self.write_candidate(root)
            data = json.loads(candidate_path.read_text(encoding="utf-8"))
            data["source"]["dirty"] = True
            candidate_path.write_text(json.dumps(data), encoding="utf-8")

            with self.assertRaisesRegex(CalibrationReportError, "clean working tree"):
                load_candidate_identity(candidate_path)


if __name__ == "__main__":
    unittest.main()
