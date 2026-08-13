from __future__ import annotations

import hashlib
import json
import tempfile
import unittest
from pathlib import Path

from scripts.stage_image_embedder_model import (
    EXPECTED_FILE_NAME,
    ModelArtifactError,
    load_manifest,
    stage_model,
    verify_file,
)


class ImageEmbedderModelStagerTest(unittest.TestCase):
    def write_manifest(self, root: Path, payload: bytes, **overrides: object) -> Path:
        manifest = {
            "schema_version": 1,
            "model_id": "test-model-v1",
            "file_name": EXPECTED_FILE_NAME,
            "sha256": hashlib.sha256(payload).hexdigest(),
            "byte_size": len(payload),
            "embedding_dimension": 1024,
            "source": {
                "url": (
                    "https://storage.googleapis.com/mediapipe-assets/tasks/testdata/vision/"
                    f"{EXPECTED_FILE_NAME}?generation=123456789"
                ),
                "manifest_revision": "a" * 40,
            },
            "license": {
                "source_repository": "example/model-source",
                "source_repository_spdx": "Apache-2.0",
            },
        }
        manifest.update(overrides)
        path = root / "manifest.json"
        path.write_text(json.dumps(manifest), encoding="utf-8")
        return path

    def test_stages_and_verifies_matching_local_source(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"reviewed model bytes"
            source = root / "source.tflite"
            destination = root / "out" / EXPECTED_FILE_NAME
            source.write_bytes(payload)
            manifest = load_manifest(self.write_manifest(root, payload))

            stage_model(manifest, destination, source_file=source)
            verify_file(destination, manifest)
            self.assertEqual(payload, destination.read_bytes())

    def test_reuses_existing_valid_file_without_trusting_new_source(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"reviewed model bytes"
            valid_source = root / "valid.tflite"
            tampered_source = root / "tampered.tflite"
            destination = root / "out" / EXPECTED_FILE_NAME
            valid_source.write_bytes(payload)
            tampered_source.write_bytes(b"tampered")
            manifest = load_manifest(self.write_manifest(root, payload))

            stage_model(manifest, destination, source_file=valid_source)
            stage_model(manifest, destination, source_file=tampered_source)

            self.assertEqual(payload, destination.read_bytes())

    def test_rejects_same_size_tampered_source_when_destination_missing(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"reviewed model bytes"
            source = root / "tampered.tflite"
            source.write_bytes(b"tampered model bytes")
            self.assertEqual(len(payload), source.stat().st_size)
            manifest = load_manifest(self.write_manifest(root, payload))

            with self.assertRaisesRegex(ModelArtifactError, "SHA-256 mismatch"):
                stage_model(manifest, root / "out" / EXPECTED_FILE_NAME, source_file=source)

    def test_rejects_wrong_size_before_hashing(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"reviewed model bytes"
            source = root / "truncated.tflite"
            source.write_bytes(b"short")
            manifest = load_manifest(self.write_manifest(root, payload))

            with self.assertRaisesRegex(ModelArtifactError, "byte-size mismatch"):
                stage_model(manifest, root / "out" / EXPECTED_FILE_NAME, source_file=source)

    def test_rejects_unpinned_source_url(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"model"
            path = self.write_manifest(root, payload)
            data = json.loads(path.read_text(encoding="utf-8"))
            data["source"]["url"] = (
                "https://storage.googleapis.com/mediapipe-assets/tasks/testdata/vision/"
                f"{EXPECTED_FILE_NAME}"
            )
            path.write_text(json.dumps(data), encoding="utf-8")

            with self.assertRaisesRegex(ModelArtifactError, "pin exactly one numeric GCS generation"):
                load_manifest(path)

    def test_rejects_wrong_host_even_with_matching_path(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"model"
            path = self.write_manifest(root, payload)
            data = json.loads(path.read_text(encoding="utf-8"))
            data["source"]["url"] = (
                "https://example.com/mediapipe-assets/tasks/testdata/vision/"
                f"{EXPECTED_FILE_NAME}?generation=123456789"
            )
            path.write_text(json.dumps(data), encoding="utf-8")

            with self.assertRaisesRegex(ModelArtifactError, "approved storage.googleapis.com host"):
                load_manifest(path)

    def test_rejects_wrong_dimension(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"model"
            path = self.write_manifest(root, payload, embedding_dimension=512)

            with self.assertRaisesRegex(ModelArtifactError, "unexpected embedding dimension"):
                load_manifest(path)

    def test_rejects_invalid_manifest_byte_size(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"model"
            path = self.write_manifest(root, payload, byte_size=0)

            with self.assertRaisesRegex(ModelArtifactError, "positive integer"):
                load_manifest(path)

    def test_rejects_non_lowercase_digest(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            payload = b"model"
            path = self.write_manifest(root, payload, sha256="A" * 64)

            with self.assertRaisesRegex(ModelArtifactError, "64 lowercase hexadecimal"):
                load_manifest(path)


if __name__ == "__main__":
    unittest.main()
