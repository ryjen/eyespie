from __future__ import annotations

import os
import subprocess
import tempfile
import unittest
from pathlib import Path


SOURCE_SHA = "a" * 40
SCRIPT = Path(__file__).resolve().parents[1] / "retain_internal_candidate.sh"


class RetainInternalCandidateTest(unittest.TestCase):
    def write_fake_gh(self, root: Path, *, existing: bool = False, draft: bool = True) -> Path:
        bin_dir = root / "bin"
        bin_dir.mkdir()
        state_dir = root / "gh-state"
        state_dir.mkdir()
        if existing:
            (state_dir / "existing").write_text("1", encoding="utf-8")
        (state_dir / "draft").write_text("true" if draft else "false", encoding="utf-8")

        gh = bin_dir / "gh"
        gh.write_text(
            """#!/usr/bin/env bash
set -euo pipefail
printf '%s\\n' "$*" >> "$FAKE_GH_LOG"
if [[ "$1" = release && "$2" = view ]]; then
  if [[ "$*" == *"--json url"* ]]; then
    echo 'https://example.invalid/releases/closed-alpha'
    exit 0
  fi
  if [[ ! -f "$FAKE_GH_STATE/existing" ]]; then
    exit 1
  fi
  if [[ "$*" == *"--json isDraft"* ]]; then
    cat "$FAKE_GH_STATE/draft"
  fi
  exit 0
fi
if [[ "$1" = release && "$2" = create ]]; then
  touch "$FAKE_GH_STATE/existing"
  exit 0
fi
if [[ "$1" = release && "$2" = edit ]]; then
  exit 0
fi
if [[ "$1" = release && "$2" = upload ]]; then
  exit 0
fi
exit 64
""",
            encoding="utf-8",
        )
        gh.chmod(0o755)
        return bin_dir

    def make_env(self, root: Path, bin_dir: Path) -> dict[str, str]:
        files = {}
        for name, payload in {
            "candidate.apk": b"apk",
            "candidate.aab": b"aab",
            "release-evidence.json": b"{}",
            "candidate.json": b"{}",
        }.items():
            path = root / name
            path.write_bytes(payload)
            files[name] = path

        summary = root / "summary.md"
        log = root / "gh.log"
        env = os.environ.copy()
        env.update(
            {
                "SOURCE_SHA": SOURCE_SHA,
                "APK": str(files["candidate.apk"]),
                "AAB": str(files["candidate.aab"]),
                "EVIDENCE": str(files["release-evidence.json"]),
                "CANDIDATE": str(files["candidate.json"]),
                "GITHUB_REPOSITORY": "ryjen/eyespie",
                "GITHUB_RUN_ID": "12345",
                "GITHUB_SERVER_URL": "https://github.com",
                "GITHUB_STEP_SUMMARY": str(summary),
                "RUNNER_TEMP": str(root / "runner-temp"),
                "FAKE_GH_STATE": str(root / "gh-state"),
                "FAKE_GH_LOG": str(log),
                "PATH": f"{bin_dir}{os.pathsep}{env['PATH']}",
            }
        )
        Path(env["RUNNER_TEMP"]).mkdir()
        return env

    def test_creates_sha_bound_draft_release_with_integrity_manifest(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            bin_dir = self.write_fake_gh(root)
            env = self.make_env(root, bin_dir)

            completed = subprocess.run(
                ["bash", str(SCRIPT)],
                check=True,
                capture_output=True,
                text=True,
                env=env,
            )

            log = Path(env["FAKE_GH_LOG"]).read_text(encoding="utf-8")
            self.assertIn(f"release create closed-alpha/android/{SOURCE_SHA}", log)
            self.assertIn(f"--target {SOURCE_SHA}", log)
            self.assertIn("--draft", log)
            self.assertIn("release_url=https://example.invalid/releases/closed-alpha", completed.stdout)

            short_sha = SOURCE_SHA[:12]
            stage = Path(env["RUNNER_TEMP"]) / f"closed-alpha-android-{short_sha}"
            sums = (stage / "SHA256SUMS").read_text(encoding="utf-8")
            self.assertIn(f"eyespie-android-{short_sha}.apk", sums)
            self.assertIn(f"eyespie-android-{short_sha}.aab", sums)
            self.assertIn("release-evidence.json", sums)
            self.assertIn("candidate.json", sums)

            summary = Path(env["GITHUB_STEP_SUMMARY"]).read_text(encoding="utf-8")
            self.assertIn("GitHub Actions artifact storage used: `no`", summary)

    def test_refuses_to_overwrite_non_draft_release(self) -> None:
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            bin_dir = self.write_fake_gh(root, existing=True, draft=False)
            env = self.make_env(root, bin_dir)

            completed = subprocess.run(
                ["bash", str(SCRIPT)],
                check=False,
                capture_output=True,
                text=True,
                env=env,
            )

            self.assertNotEqual(0, completed.returncode)
            self.assertIn("refusing to overwrite non-draft release", completed.stderr)
            log = Path(env["FAKE_GH_LOG"]).read_text(encoding="utf-8")
            self.assertNotIn("release upload", log)


if __name__ == "__main__":
    unittest.main()
