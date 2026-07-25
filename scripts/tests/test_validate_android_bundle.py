from __future__ import annotations

import tempfile
import unittest
import zipfile
from pathlib import Path

from scripts.validate_android_bundle import ValidationError, validate_bundle, validate_inventory

VALID_MANIFEST = """<?xml version=\"1.0\" encoding=\"utf-8\"?>
<manifest xmlns:dist=\"http://schemas.android.com/apk/distribution\">
  <dist:module dist:type=\"asset-pack\">
    <dist:delivery><dist:on-demand /></dist:delivery>
  </dist:module>
</manifest>
"""


class InventoryValidationTest(unittest.TestCase):
    def test_accepts_valid_topology(self) -> None:
        validate_inventory(
            {
                "base/manifest/AndroidManifest.xml",
                "base/dex/classes.dex",
                "model_pack/manifest/AndroidManifest.xml",
                "model_pack/assets/model/manifest.json",
            },
            VALID_MANIFEST,
        )

    def test_rejects_missing_model_pack(self) -> None:
        with self.assertRaisesRegex(ValidationError, "model_pack is absent"):
            validate_inventory({"base/manifest/AndroidManifest.xml"}, VALID_MANIFEST)

    def test_rejects_missing_model_manifest(self) -> None:
        with self.assertRaisesRegex(ValidationError, "required model manifest is absent"):
            validate_inventory(
                {
                    "base/manifest/AndroidManifest.xml",
                    "model_pack/manifest/AndroidManifest.xml",
                },
                VALID_MANIFEST,
            )

    def test_rejects_base_model_leakage(self) -> None:
        with self.assertRaisesRegex(ValidationError, "leaked into the base module"):
            validate_inventory(
                {
                    "base/manifest/AndroidManifest.xml",
                    "base/assets/model/model.task",
                    "model_pack/manifest/AndroidManifest.xml",
                    "model_pack/assets/model/manifest.json",
                },
                VALID_MANIFEST,
            )

    def test_rejects_non_on_demand_manifest(self) -> None:
        install_time = VALID_MANIFEST.replace("<dist:on-demand />", "<dist:install-time />")
        with self.assertRaisesRegex(ValidationError, "on-demand delivery evidence"):
            validate_inventory(
                {
                    "base/manifest/AndroidManifest.xml",
                    "model_pack/manifest/AndroidManifest.xml",
                    "model_pack/assets/model/manifest.json",
                },
                install_time,
            )


class BundleValidationTest(unittest.TestCase):
    def test_writes_stable_size_report(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            aab = root / "app-debug.aab"
            manifest = root / "model-pack-manifest.xml"
            report = root / "bundle-report.md"
            manifest.write_text(VALID_MANIFEST, encoding="utf-8")
            with zipfile.ZipFile(aab, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                archive.writestr("base/manifest/AndroidManifest.xml", b"base")
                archive.writestr("model_pack/manifest/AndroidManifest.xml", b"pack")
                archive.writestr("model_pack/assets/model/manifest.json", b"{}")

            validate_bundle(aab, manifest, report)

            content = report.read_text(encoding="utf-8")
            self.assertIn("# Android App Bundle topology", content)
            self.assertIn("`base`", content)
            self.assertIn("`model_pack`", content)
            self.assertIn("Total archive size", content)


if __name__ == "__main__":
    unittest.main()
