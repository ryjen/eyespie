from __future__ import annotations

import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from scripts.stage_image_embedding_fixtures import (
    FixtureArtifactError,
    load_manifest,
    stage_fixture,
    verify_file,
)


class ImageEmbeddingFixtureStagerTest(unittest.TestCase):
    def write_manifest(self, root: Path, payloads: dict[str, bytes]) -> Path:
        entries = [
            ("burger", "reference", "burger.jpg", "1782184424868164"),
            ("burger_crop", "related", "burger_crop.jpg", "1782184432033088"),
            ("burger_rotated", "related", "burger_rotated.jpg", "1782184439219970"),
            ("cat", "unrelated", "cat.jpg", "1782184446508641"),
        ]
        manifest = {
            "schema_version": 1,
            "source_repository": "https://github.com/ryjen/mediapipe",
            "source_revision": "a" * 40,
            "license_spdx": "Apache-2.0",
            "fixtures": [
                {
                    "id": fixture_id,
                    "role": role,
                    "file_name": file_name,
                    "sha256": hashlib.sha256(payloads[file_name]).hexdigest(),
                    "url": (
                        "https://storage.googleapis.com/mediapipe-assets/tasks/testdata/vision/"
                        f"{file_name}?generation={generation}"
                    ),
                }
                for fixture_id, role, file_name, generation in entries
            ],
        }
        path = root / "fixtures.json"
        path.write_text(json.dumps(manifest), encoding="utf-8")
        return path

    def test_manifest_and_staging_are_digest_bound(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payloads = {
                "burger.jpg": b"burger",
                "burger_crop.jpg": b"crop",
                "burger_rotated.jpg": b"rotated",
                "cat.jpg": b"cat",
            }
            manifest = load_manifest(self.write_manifest(root, payloads))
            fixture = manifest.fixtures[0]
            destination = root / fixture.file_name

            stage_fixture(
                fixture,
                destination,
                downloader=lambda _url, output: output.write_bytes(payloads[fixture.file_name]),
            )
            verify_file(destination, fixture)
            self.assertEqual(payloads[fixture.file_name], destination.read_bytes())

    def test_rejects_unpinned_url(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payloads = {name: name.encode() for name in (
                "burger.jpg", "burger_crop.jpg", "burger_rotated.jpg", "cat.jpg"
            )}
            path = self.write_manifest(root, payloads)
            data = json.loads(path.read_text(encoding="utf-8"))
            data["fixtures"][0]["url"] = (
                "https://storage.googleapis.com/mediapipe-assets/tasks/testdata/vision/burger.jpg"
            )
            path.write_text(json.dumps(data), encoding="utf-8")

            with self.assertRaisesRegex(FixtureArtifactError, "pin exactly one numeric GCS generation"):
                load_manifest(path)

    def test_rejects_wrong_role_or_order(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payloads = {name: name.encode() for name in (
                "burger.jpg", "burger_crop.jpg", "burger_rotated.jpg", "cat.jpg"
            )}
            path = self.write_manifest(root, payloads)
            data = json.loads(path.read_text(encoding="utf-8"))
            data["fixtures"][1]["role"] = "unrelated"
            path.write_text(json.dumps(data), encoding="utf-8")

            with self.assertRaisesRegex(FixtureArtifactError, "unexpected fixture identity/role"):
                load_manifest(path)

    def test_rejects_tampered_download(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payloads = {name: name.encode() for name in (
                "burger.jpg", "burger_crop.jpg", "burger_rotated.jpg", "cat.jpg"
            )}
            fixture = load_manifest(self.write_manifest(root, payloads)).fixtures[0]

            with self.assertRaisesRegex(FixtureArtifactError, "SHA-256 mismatch"):
                stage_fixture(
                    fixture,
                    root / fixture.file_name,
                    downloader=lambda _url, output: output.write_bytes(b"tampered"),
                )


if __name__ == "__main__":
    unittest.main()
