"""Protect build/evidence/cleanup ordering in ordinary Eyespie CI."""
from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parents[2] / ".github" / "workflows"


class CleanCIContract(unittest.TestCase):
    def verify_finalizer(self, file, producer, upload, guard):
        s = (ROOT / file).read_text()
        self.assertIn(producer, s)
        self.assertIn(upload, s)
        clean = s.index("name: Clean task-local Gradle outputs")
        self.assertGreater(clean, s.index(upload))
        self.assertIn("run: ./gradlew clean --no-daemon --stacktrace", s[clean:])
        self.assertIn("always()", s[clean:])
        self.assertIn(guard, s[clean:])
        if file != "main.yml":
            self.assertIn("if-no-files-found: error", s[:clean])

    def test_offline_core(self):
        self.verify_finalizer("main.yml", "run: mise run ci",
                              "name: Preserve failed Android verification diagnostics",
                              "steps.diagnostics.outcome == 'success'")

    def test_feature_screen_instrumentation(self):
        self.verify_finalizer("android-screen-instrumentation.yml",
                              "name: Run feature screen instrumentation",
                              "name: Upload installed-build visual evidence",
                              "steps.diagnostics.outcome == 'success'")

    def test_visual_references(self):
        self.verify_finalizer("wayfinder-visual-references.yml",
                              "name: Verify deterministic Wayfinder references",
                              "name: Upload visual diff diagnostics",
                              "steps.diagnostics.outcome == 'success'")

    def test_signed_distribution_scope(self):
        s = (ROOT / "internal-distribution.yml").read_text()
        self.assertNotIn("Clean task-local Gradle outputs", s)


if __name__ == "__main__":
    unittest.main()
