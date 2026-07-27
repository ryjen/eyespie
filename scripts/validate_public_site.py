#!/usr/bin/env python3
"""Validate the dependency-free Eyespie public site before deployment."""

from __future__ import annotations

import subprocess
import sys
from html.parser import HTMLParser
from pathlib import Path
from urllib.parse import unquote, urlsplit

ROOT = Path(__file__).resolve().parents[1]
SITE = ROOT / "site"
INDEX = SITE / "index.html"
REQUIRED_FILES = (
    INDEX,
    SITE / "styles.css",
    SITE / "site.js",
    SITE / ".nojekyll",
)


class SiteDocumentParser(HTMLParser):
    def __init__(self) -> None:
        super().__init__(convert_charrefs=True)
        self.ids: list[str] = []
        self.references: list[tuple[str, str]] = []

    def handle_starttag(self, tag: str, attrs: list[tuple[str, str | None]]) -> None:
        attributes = dict(attrs)
        element_id = attributes.get("id")
        if element_id:
            self.ids.append(element_id)

        for attribute in ("href", "src"):
            value = attributes.get(attribute)
            if value:
                self.references.append((attribute, value))


def fail(message: str) -> None:
    print(f"site validation failed: {message}", file=sys.stderr)
    raise SystemExit(1)


def validate_required_files() -> None:
    missing = [str(path.relative_to(ROOT)) for path in REQUIRED_FILES if not path.is_file()]
    if missing:
        fail(f"missing required files: {', '.join(missing)}")


def validate_document() -> None:
    parser = SiteDocumentParser()
    parser.feed(INDEX.read_text(encoding="utf-8"))
    parser.close()

    duplicate_ids = sorted({element_id for element_id in parser.ids if parser.ids.count(element_id) > 1})
    if duplicate_ids:
        fail(f"duplicate HTML ids: {', '.join(duplicate_ids)}")

    known_ids = set(parser.ids)
    problems: list[str] = []

    for attribute, raw_reference in parser.references:
        parsed = urlsplit(raw_reference)
        if parsed.scheme or parsed.netloc or raw_reference.startswith(("mailto:", "tel:")):
            continue

        if parsed.path:
            relative_path = unquote(parsed.path)
            candidate = (SITE / relative_path.lstrip("/")).resolve()
            try:
                candidate.relative_to(SITE.resolve())
            except ValueError:
                problems.append(f"{attribute} escapes site directory: {raw_reference}")
                continue
            if not candidate.is_file():
                problems.append(f"missing local asset for {attribute}: {raw_reference}")

        if parsed.fragment and unquote(parsed.fragment) not in known_ids:
            problems.append(f"missing fragment target: #{parsed.fragment}")

    if problems:
        fail("; ".join(problems))


def validate_javascript() -> None:
    try:
        subprocess.run(
            ["node", "--check", str(SITE / "site.js")],
            cwd=ROOT,
            check=True,
            text=True,
        )
    except FileNotFoundError:
        fail("node is unavailable for JavaScript syntax validation")
    except subprocess.CalledProcessError as error:
        fail(f"JavaScript syntax check exited with status {error.returncode}")


def main() -> None:
    validate_required_files()
    validate_document()
    validate_javascript()
    print("public site validation passed")


if __name__ == "__main__":
    main()
