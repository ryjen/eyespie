from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path

from scripts.compare_image_embedding_calibration import (
    CalibrationReportError,
    load_candidate_identity,
)
from scripts.release_candidate_identity import build_identity


class CandidateIdentitySchemaTest(unittest.TestCase):
    def test_rejects_schema_v1_candidate_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "candidate-v1.json"
            payload = build_identity(allow_dirty=True)
            payload["source"]["dirty"] = False
            payload["candidate_identity_schema_version"] = 1
            path.write_text(json.dumps(payload), encoding="utf-8")

            with self.assertRaisesRegex(
                CalibrationReportError,
                "candidate_identity_schema_version",
            ):
                load_candidate_identity(path)


if __name__ == "__main__":
    unittest.main()
