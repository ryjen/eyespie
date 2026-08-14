from __future__ import annotations

import json
import math
import tempfile
import unittest
from pathlib import Path

from scripts.compare_image_embedding_calibration import (
    EXPECTED_FIXTURE_SHA256,
    EXPECTED_MODEL_SHA256,
    CalibrationReportError,
    compare_reports,
    load_report,
    require_same_fixture_cross_platform_match,
)


class ImageEmbeddingCalibrationComparatorTest(unittest.TestCase):
    def vector(self, *components: float) -> list[float]:
        return list(components) + [0.0] * (1024 - len(components))

    def write_report(
        self,
        root: Path,
        platform: str,
        *,
        model_sha256: str = EXPECTED_MODEL_SHA256,
        match_threshold: float = 0.5,
        non_finite: bool = False,
        reference: tuple[float, float] = (1.0, 0.0),
        crop: tuple[float, float] = (0.98, 0.02),
        rotated: tuple[float, float] = (0.95, 0.05),
        cat: tuple[float, float] = (0.0, 1.0),
    ) -> Path:
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
            "report_schema_version": 1,
            "platform": platform,
            "device": {"manufacturer": "test", "model": platform, "os": "test-os"},
            "runtime": {"name": f"runtime-{platform}", "version": "1.0"},
            "model": {
                "id": "mobilenet-v3-small-100-224-embedder",
                "sha256": model_sha256,
            },
            "match_policy": {"cosine_threshold": match_threshold},
            "embedding_contract": {"schema_version": 1, "dimensions": 1024},
            "fixtures": [
                {
                    "id": fixture_id,
                    "role": role,
                    "source_sha256": EXPECTED_FIXTURE_SHA256[fixture_id],
                    "embedding": vector,
                    "repeat_count": 5,
                    "repeat_cosine_min": 1.0,
                    "repeat_max_abs_delta": 0.0,
                }
                for fixture_id, role, vector in fixtures
            ],
        }
        path = root / f"{platform}.json"
        path.write_text(json.dumps(payload), encoding="utf-8")
        return path

    def test_compares_observed_behavior_without_changing_threshold(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            android = load_report(self.write_report(root, "android"))
            ios = load_report(self.write_report(root, "ios"))

            result = compare_reports(android, ios)

            self.assertEqual(1.0, result["cross_platform"]["burger"]["cosine_similarity"])
            self.assertEqual(0.0, result["cross_platform"]["burger"]["rmse"])
            self.assertTrue(
                result["cross_platform"]["burger"]["matches_configured_threshold"]
            )
            self.assertTrue(result["same_fixture_cross_platform_match"]["all_match"])
            self.assertEqual(
                EXPECTED_FIXTURE_SHA256["burger"],
                result["fixtures"]["burger"]["sha256"],
            )
            self.assertGreater(
                result["android"]["semantic_behavior"]["burger_crop"]["cosine_similarity"],
                result["android"]["semantic_behavior"]["cat"]["cosine_similarity"],
            )
            self.assertTrue(
                result["cross_platform_semantic_behavior"]["android_reference_to_ios"]
                ["burger_crop"]["matches_configured_threshold"]
            )
            self.assertTrue(
                result["cross_platform_semantic_behavior"]["ios_reference_to_android"]
                ["burger_crop"]["matches_configured_threshold"]
            )
            self.assertFalse(
                result["android"]["semantic_behavior"]["cat"]["matches_configured_threshold"]
            )
            self.assertTrue(result["match_decision_consistency"]["all_consistent"])
            self.assertFalse(result["match_policy"]["threshold_changed"])
            self.assertEqual(0.5, result["match_policy"]["cosine_threshold"])
            require_same_fixture_cross_platform_match(result)

    def test_reports_inconsistent_within_platform_match_decision(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            android = load_report(self.write_report(root, "android", crop=(0.8, 0.6)))
            ios = load_report(self.write_report(root, "ios", crop=(0.4, 0.916515138991168)))

            result = compare_reports(android, ios)

            self.assertFalse(
                result["match_decision_consistency"]["fixtures"]["burger_crop"]
            )
            self.assertFalse(result["match_decision_consistency"]["all_consistent"])

    def test_detects_basis_shift_that_only_breaks_cross_platform_matching(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            android = load_report(
                self.write_report(
                    root,
                    "android",
                    match_threshold=0.8,
                    reference=(1.0, 0.0),
                    crop=(1.0, 0.0),
                    rotated=(1.0, 0.0),
                )
            )
            ios = load_report(
                self.write_report(
                    root,
                    "ios",
                    match_threshold=0.8,
                    reference=(0.7, 0.714142842),
                    crop=(0.7, 0.714142842),
                    rotated=(0.7, 0.714142842),
                )
            )

            result = compare_reports(android, ios)

            self.assertTrue(
                result["android"]["semantic_behavior"]["burger_crop"]
                ["matches_configured_threshold"]
            )
            self.assertTrue(
                result["ios"]["semantic_behavior"]["burger_crop"]
                ["matches_configured_threshold"]
            )
            self.assertFalse(
                result["cross_platform_semantic_behavior"]["android_reference_to_ios"]
                ["burger_crop"]["matches_configured_threshold"]
            )
            self.assertFalse(
                result["cross_platform_semantic_behavior"]["ios_reference_to_android"]
                ["burger_crop"]["matches_configured_threshold"]
            )
            self.assertFalse(
                result["same_fixture_cross_platform_match"]["fixtures"]["burger"]
            )
            self.assertFalse(result["same_fixture_cross_platform_match"]["all_match"])
            self.assertFalse(
                result["match_decision_consistency"]["fixtures"]["burger_crop"]
            )
            self.assertFalse(result["match_decision_consistency"]["all_consistent"])
            with self.assertRaisesRegex(
                CalibrationReportError,
                "same-fixture cross-platform matching failed.*burger",
            ):
                require_same_fixture_cross_platform_match(result)

    def test_rejects_unpinned_packaged_model(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = self.write_report(root, "android", model_sha256="b" * 64)

            with self.assertRaisesRegex(CalibrationReportError, "pinned image-embedding model"):
                load_report(path)

    def test_rejects_unpinned_fixture_bytes(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = self.write_report(root, "android")
            data = json.loads(path.read_text(encoding="utf-8"))
            data["fixtures"][0]["source_sha256"] = "b" * 64
            path.write_text(json.dumps(data), encoding="utf-8")

            with self.assertRaisesRegex(CalibrationReportError, "pinned calibration fixture"):
                load_report(path)

    def test_rejects_different_configured_thresholds(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            android = load_report(self.write_report(root, "android", match_threshold=0.5))
            ios = load_report(self.write_report(root, "ios", match_threshold=0.6))

            with self.assertRaisesRegex(CalibrationReportError, "different configured match thresholds"):
                compare_reports(android, ios)

    def test_rejects_non_finite_embedding_value(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = self.write_report(root, "android", non_finite=True)

            with self.assertRaisesRegex(CalibrationReportError, "must be finite"):
                load_report(path)

    def test_rejects_wrong_dimension(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            path = self.write_report(root, "android")
            data = json.loads(path.read_text(encoding="utf-8"))
            data["fixtures"][0]["embedding"] = data["fixtures"][0]["embedding"][:-1]
            path.write_text(json.dumps(data), encoding="utf-8")

            with self.assertRaisesRegex(CalibrationReportError, "exactly 1024"):
                load_report(path)


if __name__ == "__main__":
    unittest.main()
