from pathlib import Path
import re
import unittest


ROOT = Path(__file__).resolve().parents[2]
BUILD = ROOT / "eyespie/build.gradle.kts"
IDENTITY = ROOT / "eyespie/src/androidMain/kotlin/com/micrantha/eyespie/telemetry/AndroidDiagnosticIdentity.kt"
DISTRIBUTION = ROOT / ".github/workflows/internal-distribution.yml"


class AndroidDiagnosticSourceRevisionTest(unittest.TestCase):
    def test_runtime_source_revision_comes_from_protected_source_sha(self) -> None:
        build = BUILD.read_text(encoding="utf-8")
        identity = IDENTITY.read_text(encoding="utf-8")
        distribution = DISTRIBUTION.read_text(encoding="utf-8")

        self.assertIn('providers.environmentVariable("SOURCE_SHA")', build)
        self.assertIn('Regex("[0-9a-f]{40}")', build)
        self.assertIn('"SOURCE_REVISION"', build)
        self.assertIn('BuildConfig.SOURCE_REVISION.takeIf(String::isNotBlank)', identity)

        # Both protected platform jobs use the same workflow input, and the
        # workflow proves the checkout matches it before release evidence/build.
        self.assertGreaterEqual(
            distribution.count("SOURCE_SHA: ${{ inputs.source_sha }}"),
            2,
        )
        self.assertGreaterEqual(
            distribution.count('test "$(git rev-parse HEAD)" = "$SOURCE_SHA"'),
            2,
        )
        self.assertIn(
            '[[ "$SOURCE_SHA" =~ ^[0-9a-f]{40}$ ]]',
            distribution,
        )

    def test_no_alternate_android_source_revision_provider_is_introduced(self) -> None:
        build = BUILD.read_text(encoding="utf-8")
        declarations = re.findall(r'environmentVariable\("([A-Z0-9_]+)"\)', build)
        source_variables = [name for name in declarations if "SHA" in name or "REVISION" in name]
        self.assertEqual(["SOURCE_SHA"], source_variables)


if __name__ == "__main__":
    unittest.main()
