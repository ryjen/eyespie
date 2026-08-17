from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

import scripts.release_candidate_identity as candidate_identity
from scripts.compare_image_embedding_calibration import (
    CalibrationReportError,
    load_candidate_identity,
)
from scripts.release_candidate_identity import (
    CandidateIdentityError,
    build_identity,
)


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

    def test_rejects_xcode_team_suffix_in_bundle_identifier(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "project.pbxproj"
            current = candidate_identity.read_text(candidate_identity.XCODE_PROJECT)
            self.assertIn('PRODUCT_BUNDLE_IDENTIFIER = "${BUNDLE_ID}";', current)
            path.write_text(
                current.replace(
                    'PRODUCT_BUNDLE_IDENTIFIER = "${BUNDLE_ID}";',
                    'PRODUCT_BUNDLE_IDENTIFIER = "${BUNDLE_ID}.ios.${TEAM_ID}";',
                ),
                encoding="utf-8",
            )

            with patch.object(candidate_identity, "XCODE_PROJECT", path):
                with self.assertRaisesRegex(
                    CandidateIdentityError,
                    "bundle identifiers must inherit canonical BUNDLE_ID",
                ):
                    candidate_identity.verify_wiring(
                        "0.1.0",
                        1,
                        candidate_identity.ios_mediapipe_version(),
                    )

    def test_rejects_noncanonical_ios_bundle_id_config(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            path = Path(directory) / "Config.debug.xcconfig"
            current = candidate_identity.read_text(candidate_identity.DEBUG_XCCONFIG)
            self.assertIn("BUNDLE_ID=com.micrantha.eyespie", current)
            path.write_text(
                current.replace(
                    "BUNDLE_ID=com.micrantha.eyespie",
                    "BUNDLE_ID=com.example.wrong",
                ),
                encoding="utf-8",
            )

            with patch.object(candidate_identity, "DEBUG_XCCONFIG", path):
                with self.assertRaisesRegex(
                    CandidateIdentityError,
                    "must define canonical BUNDLE_ID",
                ):
                    candidate_identity.verify_wiring(
                        "0.1.0",
                        1,
                        candidate_identity.ios_mediapipe_version(),
                    )


if __name__ == "__main__":
    unittest.main()
