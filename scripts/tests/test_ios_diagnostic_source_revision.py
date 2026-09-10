from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
INFO = ROOT / "iosApp/iosApp/Info.plist"
DEBUG_CONFIG = ROOT / "iosApp/Configuration/Config.debug.xcconfig"
RELEASE_CONFIG = ROOT / "iosApp/Configuration/Config.release.xcconfig"
SOURCE_CONFIG = ROOT / "iosApp/Configuration/SourceRevision.xcconfig"
IDENTITY = ROOT / "eyespie/src/iosMain/kotlin/com/micrantha/eyespie/telemetry/IosDiagnosticIdentity.kt"
DISTRIBUTION = ROOT / ".github/workflows/internal-distribution.yml"
EVIDENCE = ROOT / "scripts/internal_release_evidence.py"


class IosDiagnosticSourceRevisionTest(unittest.TestCase):
    def test_runtime_source_revision_comes_from_protected_source_sha(self) -> None:
        info = INFO.read_text(encoding="utf-8")
        debug = DEBUG_CONFIG.read_text(encoding="utf-8")
        release = RELEASE_CONFIG.read_text(encoding="utf-8")
        source = SOURCE_CONFIG.read_text(encoding="utf-8")
        identity = IDENTITY.read_text(encoding="utf-8")
        distribution = DISTRIBUTION.read_text(encoding="utf-8")
        evidence = EVIDENCE.read_text(encoding="utf-8")

        self.assertIn('<key>EyespieSourceRevision</key>', info)
        self.assertIn('<string>$(EYESPIE_SOURCE_REVISION)</string>', info)
        self.assertIn('#include "SourceRevision.xcconfig"', debug)
        self.assertIn('#include "SourceRevision.xcconfig"', release)
        self.assertIn("EYESPIE_SOURCE_REVISION=", source)
        self.assertIn('optionalBundleString(bundle, "EyespieSourceRevision")', identity)

        self.assertIn("SOURCE_SHA: ${{ inputs.source_sha }}", distribution)
        self.assertIn('test "$(git rev-parse HEAD)" = "$SOURCE_SHA"', distribution)
        self.assertIn('EYESPIE_SOURCE_REVISION="$SOURCE_SHA"', distribution)
        self.assertIn("Print :EyespieSourceRevision", distribution)
        self.assertIn('"source_revision": os.environ["IOS_METADATA_SOURCE_REVISION"]', distribution)

        self.assertIn('source_revision = metadata.get("source_revision")', evidence)
        self.assertIn('source_revision != candidate["source"]["commit_sha"]', evidence)
        self.assertIn('"embedded_source_revision": source_revision', evidence)

    def test_local_ios_builds_do_not_fabricate_source_identity(self) -> None:
        source_lines = [
            line.strip()
            for line in SOURCE_CONFIG.read_text(encoding="utf-8").splitlines()
            if line.strip() and not line.lstrip().startswith("//")
        ]
        self.assertEqual(["EYESPIE_SOURCE_REVISION="], source_lines)


if __name__ == "__main__":
    unittest.main()
