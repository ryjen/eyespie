from __future__ import annotations

import tempfile
import unittest
import zipfile
from pathlib import Path

from scripts.validate_android_bundle import ValidationError, validate_bundle, validate_inventory

DIST = "http://schemas.android.com/apk/distribution"


def manifest(mode: str = "on-demand") -> str:
    return f'''<?xml version="1.0" encoding="utf-8"?>
<manifest xmlns:dist="{DIST}">
  <dist:module dist:type="asset-pack">
    <dist:delivery><dist:{mode} /></dist:delivery>
  </dist:module>
</manifest>
'''


VALID_ENTRIES = {
    "base/manifest/AndroidManifest.xml",
    "base/dex/classes.dex",
    "model_pack/manifest/AndroidManifest.xml",
    "model_pack/assets/model/manifest.json",
}


class InventoryValidationTest(unittest.TestCase):
    def test_accepts_valid_topology(self) -> None:
        validate_inventory(VALID_ENTRIES, manifest())

    def test_rejects_missing_model_pack(self) -> None:
        with self.assertRaisesRegex(ValidationError, "model_pack is absent"):
            validate_inventory({"base/manifest/AndroidManifest.xml"}, manifest())

    def test_rejects_missing_model_manifest(self) -> None:
        with self.assertRaisesRegex(ValidationError, "required model manifest is absent"):
            validate_inventory(
                {
                    "base/manifest/AndroidManifest.xml",
                    "model_pack/manifest/AndroidManifest.xml",
                },
                manifest(),
            )

    def test_rejects_base_model_leakage(self) -> None:
        with self.assertRaisesRegex(ValidationError, "leaked into the base module"):
            validate_inventory(
                VALID_ENTRIES | {"base/assets/model/model.task"},
                manifest(),
            )

    def test_rejects_install_time_delivery(self) -> None:
        with self.assertRaisesRegex(ValidationError, "found install-time"):
            validate_inventory(VALID_ENTRIES, manifest("install-time"))

    def test_rejects_fast_follow_delivery(self) -> None:
        with self.assertRaisesRegex(ValidationError, "found fast-follow"):
            validate_inventory(VALID_ENTRIES, manifest("fast-follow"))

    def test_rejects_missing_delivery(self) -> None:
        missing_delivery = f'''<manifest xmlns:dist="{DIST}">
  <dist:module dist:type="asset-pack" />
</manifest>'''
        with self.assertRaisesRegex(ValidationError, "missing dist:delivery"):
            validate_inventory(VALID_ENTRIES, missing_delivery)

    def test_rejects_ambiguous_delivery(self) -> None:
        ambiguous = f'''<manifest xmlns:dist="{DIST}">
  <dist:module dist:type="asset-pack">
    <dist:delivery>
      <dist:on-demand />
      <dist:fast-follow />
    </dist:delivery>
  </dist:module>
</manifest>'''
        with self.assertRaisesRegex(ValidationError, "exactly one delivery mode"):
            validate_inventory(VALID_ENTRIES, ambiguous)

    def test_rejects_non_asset_pack_module(self) -> None:
        feature_module = manifest().replace('dist:type="asset-pack"', 'dist:type="feature"')
        with self.assertRaisesRegex(ValidationError, "must be asset-pack"):
            validate_inventory(VALID_ENTRIES, feature_module)


class BundleValidationTest(unittest.TestCase):
    def test_writes_stable_size_report(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            aab = root / "app-debug.aab"
            decoded_manifest = root / "model-pack-manifest.xml"
            report = root / "bundle-report.md"
            decoded_manifest.write_text(manifest(), encoding="utf-8")
            with zipfile.ZipFile(aab, "w", compression=zipfile.ZIP_DEFLATED) as archive:
                archive.writestr("base/manifest/AndroidManifest.xml", b"base")
                archive.writestr("model_pack/manifest/AndroidManifest.xml", b"pack")
                archive.writestr("model_pack/assets/model/manifest.json", b"{}")

            validate_bundle(aab, decoded_manifest, report)

            content = report.read_text(encoding="utf-8")
            self.assertIn("# Android App Bundle topology", content)
            self.assertIn("`base`", content)
            self.assertIn("`model_pack`", content)
            self.assertIn("Total archive size", content)
            self.assertIn("structurally configured", content)


if __name__ == "__main__":
    unittest.main()
