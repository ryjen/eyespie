"""Ensure internal signing accepts only source reachable from approved main history."""
from __future__ import annotations

import os
import re
import subprocess
import tempfile
import textwrap
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
WORKFLOW = ROOT / ".github/workflows/internal-distribution.yml"


def authorization_scripts() -> list[str]:
    source = WORKFLOW.read_text(encoding="utf-8")
    marker = "      - name: Authorize candidate against protected main history\n"
    parts = source.split(marker)
    if len(parts) != 3:
        raise AssertionError("expected one trusted authorization gate per platform")
    scripts = []
    for segment in parts[1:]:
        if not segment.startswith("        shell: bash\n        run: |\n"):
            raise AssertionError("authorization must be an unconditional bash step")
        body = segment.split("        run: |\n", 1)[1]
        lines = []
        for line in body.splitlines():
            if line and not line.startswith("          "):
                break
            if not line:
                break
            lines.append(line[10:])
        scripts.append("\n".join(lines) + "\n")
    return scripts


def git(root: Path, *args: str) -> str:
    return subprocess.check_output(
        ["git", "-C", str(root), *args], stderr=subprocess.DEVNULL, text=True
    ).strip()


class InternalDistributionSourceGateTests(unittest.TestCase):
    def test_both_jobs_have_identical_gate_before_build_tools(self):
        workflow = WORKFLOW.read_text(encoding="utf-8")
        scripts = authorization_scripts()
        self.assertEqual(scripts[0], scripts[1])
        self.assertEqual(workflow.count("fetch-depth: 0 # full history"), 2)
        self.assertEqual(
            workflow.count("Authorize candidate against protected main history"), 2
        )
        for job in workflow.split("      - name: Checkout exact candidate\n")[1:]:
            self.assertLess(
                job.index("Authorize candidate against protected main history"),
                job.index("Set up JDK 21"),
            )

    def test_main_ancestor_allowed_other_refs_rejected(self):
        with tempfile.TemporaryDirectory() as directory:
            root = Path(directory)
            git(root, "init", "-q", "-b", "main")
            git(root, "config", "user.email", "noreply@example.test")
            git(root, "config", "user.name", "Synthetic Test")
            (root / "safe.txt").write_text("approved\n")
            git(root, "add", "safe.txt")
            git(root, "commit", "-qm", "approved source")
            main_sha = git(root, "rev-parse", "HEAD")
            # Local synthetic tracking ref; no network or GitHub access.
            git(root, "update-ref", "refs/remotes/origin/main", main_sha)
            git(root, "checkout", "-qb", "unapproved")
            (root / "safe.txt").write_text("unreviewed branch\n")
            git(root, "commit", "-qam", "unreviewed source")
            unapproved_sha = git(root, "rev-parse", "HEAD")
            script = authorization_scripts()[0]

            def verify(sha: str, dispatch_ref="refs/heads/main", repo="ryjen/eyespie"):
                env = os.environ.copy()
                env.update(GITHUB_REPOSITORY=repo, GITHUB_REF=dispatch_ref, SOURCE_SHA=sha)
                return subprocess.run(
                    ["bash", "-c", script], cwd=root, env=env,
                    stdout=subprocess.PIPE, stderr=subprocess.PIPE, text=True,
                    check=False,
                ).returncode

            git(root, "checkout", "-q", "main")
            self.assertEqual(verify(main_sha), 0)
            self.assertNotEqual(verify(main_sha, repo="example/other"), 0)
            self.assertNotEqual(verify(main_sha, dispatch_ref="refs/heads/topic"), 0)
            git(root, "checkout", "-q", "unapproved")
            self.assertNotEqual(verify(unapproved_sha), 0)
            self.assertNotEqual(verify(main_sha), 0)


if __name__ == "__main__":
    unittest.main()
