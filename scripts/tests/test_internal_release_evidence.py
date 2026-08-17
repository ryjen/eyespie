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
ANDROID_SIGNER_SHA = "c" * 64
IOS_TEAM_ID = "FKL5L3E8N8"
IOS_APPLICATION_ID = f"{IOS_TEAM_ID}.com.micrantha.eyespie"


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


def android_metadata(**overrides) -> dict:
    payload = {
        "package_id": "com.micrantha.eyespie",
        "version": "0.1.0",
        "build": 1,
        "signer_certificate_sha256": ANDROID_SIGNER_SHA,
        "permissions": ["android.permission.CAMERA"],
    }
    payload.update(overrides)
    return payload


def ios_metadata(**overrides) -> dict:
    payload = {
        "bundle_id": "com.micrantha.eyespie",
        "version": "0.1.0",
        "build": "1",
        "mediapipe_version": "0.10.26.2",
        "signing_team_id": IOS_TEAM_ID,
        "application_identifier": IOS_APPLICATION_ID,
    }
    payload.update(overrides)
    return payload


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
            with patch("scripts.internal_release_evidence._git_head", return_value="d" * 40):
                with self.assertRaisesRegex(ReleaseEvidenceError, "checked-out HEAD"):
                    verify_source(candidate, SOURCE_SHA)

    def test_android_binds_artifact_signer_and_rejects_internet_permission(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            metadata = android_metadata()
            metadata_path = self.write_json(root, "android.json", metadata)
            apk = root / "app.apk"
            aab = root / "app.aab"
            apk.write_bytes(b"apk")
            aab.write_bytes(b"aab")

            evidence = validate_android(candidate, metadata_path, apk, aab)
            self.assertEqual("android", evidence["platform"])
            self.assertEqual("play-internal", evidence["channel"])
            self.assertEqual(ANDROID_SIGNER_SHA, evidence["signing"]["certificate_sha256"])
            self.assertEqual(64, len(evidence["artifacts"]["apk"]["sha256"]))
            self.assertNotIn("path", json.dumps(evidence))

            metadata["permissions"].append("android.permission.INTERNET")
            metadata_path = self.write_json(root, "android-internet.json", metadata)
            with self.assertRaisesRegex(ReleaseEvidenceError, "INTERNET"):
                validate_android(candidate, metadata_path, apk, aab)

    def test_android_rejects_version_package_or_signer_drift(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            apk = root / "app.apk"
            aab = root / "app.aab"
            apk.write_bytes(b"apk")
            aab.write_bytes(b"aab")

            bad_package = self.write_json(root, "bad-package.json", android_metadata(package_id="example.invalid"))
            with self.assertRaisesRegex(ReleaseEvidenceError, "package id"):
                validate_android(candidate, bad_package, apk, aab)

            bad_version = self.write_json(root, "bad-version.json", android_metadata(version="0.2.0"))
            with self.assertRaisesRegex(ReleaseEvidenceError, "version"):
                validate_android(candidate, bad_version, apk, aab)

            bad_signer = self.write_json(root, "bad-signer.json", android_metadata(signer_certificate_sha256="not-a-digest"))
            with self.assertRaisesRegex(ReleaseEvidenceError, "signer certificate"):
                validate_android(candidate, bad_signer, apk, aab)

    def test_ios_binds_bundle_version_build_runtime_and_signing_identity(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            metadata = self.write_json(root, "ios.json", ios_metadata())
            ipa = root / "app.ipa"
            ipa.write_bytes(b"ipa")

            evidence = validate_ios(candidate, metadata, ipa)
            self.assertEqual("ios", evidence["platform"])
            self.assertEqual("testflight-internal", evidence["channel"])
            self.assertEqual("com.micrantha.eyespie", evidence["bundle_id"])
            self.assertEqual(IOS_TEAM_ID, evidence["signing"]["team_id"])
            self.assertEqual(IOS_APPLICATION_ID, evidence["signing"]["application_identifier"])

    def test_ios_rejects_bundle_runtime_or_signing_drift(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            candidate = self.write_json(root, "candidate.json", candidate_payload())
            ipa = root / "app.ipa"
            ipa.write_bytes(b"ipa")

            wrong_bundle = self.write_json(root, "wrong-bundle.json", ios_metadata(bundle_id="com.invalid"))
            with self.assertRaisesRegex(ReleaseEvidenceError, "bundle id"):
                validate_ios(candidate, wrong_bundle, ipa)

            wrong_runtime = self.write_json(root, "wrong-runtime.json", ios_metadata(mediapipe_version="9.9.9"))
            with self.assertRaisesRegex(ReleaseEvidenceError, "MediaPipe"):
                validate_ios(candidate, wrong_runtime, ipa)

            wrong_team = self.write_json(root, "wrong-team.json", ios_metadata(signing_team_id="WRONGTEAM"))
            with self.assertRaisesRegex(ReleaseEvidenceError, "team identifier"):
                validate_ios(candidate, wrong_team, ipa)

            wrong_application = self.write_json(
                root,
                "wrong-application.json",
                ios_metadata(application_identifier=f"{IOS_TEAM_ID}.com.invalid"),
            )
            with self.assertRaisesRegex(ReleaseEvidenceError, "application identifier"):
                validate_ios(candidate, wrong_application, ipa)


if __name__ == "__main__":
    unittest.main()
