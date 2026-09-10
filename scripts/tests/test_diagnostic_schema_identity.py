from __future__ import annotations

import re
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
DATABASE_SCHEMA_SOURCE = (
    ROOT
    / "eyespie/src/commonMain/kotlin/com/micrantha/eyespie/persistence/DatabaseSchema.kt"
)
SQLDELIGHT_DIR = ROOT / "eyespie/src/commonMain/sqldelight/com/micrantha/eyespie/data"


class DiagnosticDatabaseSchemaIdentityTest(unittest.TestCase):
    def test_runtime_diagnostic_schema_identity_matches_migration_set(self) -> None:
        source = DATABASE_SCHEMA_SOURCE.read_text(encoding="utf-8")
        match = re.search(
            r"const val EYESPIE_DATABASE_SCHEMA_VERSION\s*=\s*([0-9]+)",
            source,
        )
        self.assertIsNotNone(match, "diagnostic database schema constant is missing")

        migrations = sorted(
            int(path.stem)
            for path in SQLDELIGHT_DIR.glob("*.sqm")
            if path.stem.isdigit()
        )
        expected = (max(migrations) if migrations else 0) + 1
        self.assertEqual(expected, int(match.group(1)))


if __name__ == "__main__":
    unittest.main()
