from __future__ import annotations

import json
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch

from scripts.internal_release_evidence import (
    ReleaseEvidenceError,
    validate_android,
    validate_ios,
    verify_source,
)


SOURCE_SHA = "a" * 40
MODEL_SHA = "b" * 64


def candidate_payload() -> dict:
    return {
        "candidate_identity_schema_version": 2,
        "candidate": f"0.1.0+1@{SOURCE_SHA[:12]}",
        "repository": "ryjen/eyespie",
        "source": {"commit_sha": SOURCE_SHA, "dirty": False},
        "application": {"version": "0.1.0", "build": 1},
        "mediapipe": {
            "android": {"tasks_vision": "0.10.35", "tasks_genai": "0.10.35"},
            "ios": {"project_artifact_version": "0.10.26.2"},
        },
        "image_embedding": {"model_sha256": MODEL_SHA},
    }


class InternalReleaseEvidenceTest(unittest.TestCase):
    def write_json(self, root: Path, name: str, payload: dict) -> Path:
        path = root / name
        path.write_text(json.dumps(payload), encoding="utf-8")
        return path

    def test_source_requires_exact_clean_candidate_and_head(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            with patch("scripts.internal_release_evidence._git_head", return_value=SOURCE_SHA):
                parsed = verify_source(candidate, SOURCE_SHA)
            self.assertEqual(SOURCE_SHA, parsed["source"]["commit_sha"])

    def test_source_rejects_checked_out_head_drift(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            with patch("scripts.internal_release_evidence._git_head", return_value="c" * 40):
                with self.assertRaisesRegex(ReleaseEvidenceError, "checked-out HEAD"):
                    verify_source(candidate, SOURCE_SHA)

    def test_android_binds_artifact_and_rejects_internet_permission(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            metadata = {
                "package_id": "com.micrantha.eyespie",
                "version": "0.1.0",
                "build": 1,
                "permissions": ["android.permission.CAMERA"],
            }
            metadata_path = self.write_json(root, "android.json", metadata)
            apk = root / "app.apk"
            aab = root / "app.aab"
            apk.write_bytes(b"apk")
            aab.write_bytes(b"aab")

            evidence = validate_android(candidate, metadata_path, apk, aab)
            self.assertEqual("android", evidence["platform"])
            self.assertEqual("play-internal", evidence["channel"])
            self.assertEqual(64, len(evidence["artifacts"]["apk"]["sha256"]))
            self.assertNotIn("path", json.dumps(evidence))

            metadata["permissions"].append("android.permission.INTERNET")
            metadata_path = self.write_json(root, "android-internet.json", metadata)
            with self.assertRaisesRegex(ReleaseEvidenceError, "INTERNET"):
                validate_android(candidate, metadata_path, apk, aab)

    def test_android_rejects_version_or_package_drift(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            apk = root / "app.apk"
            aab = root / "app.aab"
            apk.write_bytes(b"apk")
            aab.write_bytes(b"aab")

            bad_package = self.write_json(
                root,
                "bad-package.json",
                {"package_id": "example.invalid", "version": "0.1.0", "build": 1, "permissions": []},
            )
            with self.assertRaisesRegex(ReleaseEvidenceError, "package id"):
                validate_android(candidate, bad_package, apk, aab)

            bad_version = self.write_json(
                root,
                "bad-version.json",
                {"package_id": "com.micrantha.eyespie", "version": "0.2.0", "build": 1, "permissions": []},
            )
            with self.assertRaisesRegex(ReleaseEvidenceError, "version"):
                validate_android(candidate, bad_version, apk, aab)

    def test_ios_binds_bundle_version_build_and_mediapipe(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            metadata = self.write_json(
                root,
                "ios.json",
                {
                    "bundle_id": "com.micrantha.eyespie",
                    "version": "0.1.0",
                    "build": "1",
                    "mediapipe_version": "0.10.26.2",
                },
            )
            ipa = root / "app.ipa"
            ipa.write_bytes(b"ipa")

            evidence = validate_ios(candidate, metadata, ipa)
            self.assertEqual("ios", evidence["platform"])
            self.assertEqual("testflight-internal", evidence["channel"])
            self.assertEqual("com.micrantha.eyespie", evidence["bundle_id"])

    def test_ios_rejects_bundle_or_runtime_drift(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            ipa = root / "app.ipa"
            ipa.write_bytes(b"ipa")

            wrong_bundle = self.write_json(
                root,
                "wrong-bundle.json",
                {"bundle_id": "com.invalid", "version": "0.1.0", "build": "1", "mediapipe_version": "0.10.26.2"},
            )
            with self.assertRaisesRegex(ReleaseEvidenceError, "bundle id"):
                validate_ios(candidate, wrong_bundle, ipa)

            wrong_runtime = self.write_json(
                root,
                "wrong-runtime.json",
                {"bundle_id": "com.micrantha.eyespie", "version": "0.1.0", "build": "1", "mediapipe_version": "9.9.9"},
            )
            with self.assertRaisesRegex(ReleaseEvidenceError, "MediaPipe"):
                validate_ios(candidate, wrong_runtime, ipa)


if __name__ == "__main__":
    unittest.main()
