from __future__ import annotations

import json
import math
import tempfile
import unittest
from pathlib import Path

from scripts.compare_image_embedding_calibration import (
    CalibrationReportError,
    compare_reports,
    load_report,
)


class ImageEmbeddingCalibrationComparatorTest(unittest.TestCase):
    def vector(self, *components: float) -> list[float]:
        return list(components) + [0.0] * (1024 - len(components))

    def write_report(
        self,
        root: Path,
        platform: str,
        *,
        model_sha256: str = "a" * 64,
        non_finite: bool = False,
    ) -> Path:
        reference = self.vector(1.0, 0.0)
        crop = self.vector(0.98, 0.02)
        rotated = self.vector(0.95, 0.05)
        cat = self.vector(0.0, 1.0)
        if non_finite:
            reference[5] = math.nan
        fixtures = [
            ("burger", "reference", reference),
            ("burger_crop", "related", crop),
            ("burger_rotated", "related", rotated),
            ("cat", "unrelated", cat),
        ]
        payload = {
            "report_schema_version": 1,
            "platform": platform,
            "device": {"manufacturer": "test", "model": platform, "os": "test-os"},
            "runtime": {"name": f"runtime-{platform}", "version": "1.0"},
            "model": {"id": "mobilenet-v3-small-100-224-embedder", "sha256": model_sha256},
            "embedding_contract": {"schema_version": 1, "dimensions": 1024},
            "fixtures": [
                {
                    "id": fixture_id,
                    "role": role,
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

    def test_compares_same_model_without_product_threshold(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            android = load_report(self.write_report(root, "android"))
            ios = load_report(self.write_report(root, "ios"))

            result = compare_reports(android, ios)

            self.assertEqual(1.0, result["cross_platform"]["burger"]["cosine_similarity"])
            self.assertEqual(0.0, result["cross_platform"]["burger"]["rmse"])
            self.assertGreater(
                result["android"]["semantic_cosine"]["burger_crop"],
                result["android"]["semantic_cosine"]["cat"],
            )
            self.assertFalse(result["policy"]["threshold_changed"])

    def test_rejects_different_model_identity(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            android = load_report(self.write_report(root, "android", model_sha256="a" * 64))
            ios = load_report(self.write_report(root, "ios", model_sha256="b" * 64))

            with self.assertRaisesRegex(CalibrationReportError, "different model identities"):
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
