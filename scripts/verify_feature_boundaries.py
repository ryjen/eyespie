#!/usr/bin/env python3
from __future__ import annotations

import re
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
FEATURES_ROOT = ROOT / "eyespie/src/commonMain/kotlin/com/micrantha/eyespie/features"
FEATURE_IMPORT = "import com.micrantha.eyespie.features."
APP_IMPORT = "import com.micrantha.eyespie.app"
SCREEN_SIGNATURE = re.compile(r"fun\s+\w+Screen\s*\((.*?)\)\s*\{", re.DOTALL)


def split_top_level_parameters(signature: str) -> list[str]:
    parameters: list[str] = []
    current: list[str] = []
    paren = angle = bracket = 0

    for char in signature:
        if char == "(":
            paren += 1
        elif char == ")":
            paren -= 1
        elif char == "<":
            angle += 1
        elif char == ">" and angle > 0:
            angle -= 1
        elif char == "[":
            bracket += 1
        elif char == "]":
            bracket -= 1

        if char == "," and paren == 0 and angle == 0 and bracket == 0:
            parameter = "".join(current).strip()
            if parameter:
                parameters.append(parameter)
            current = []
        else:
            current.append(char)

    parameter = "".join(current).strip()
    if parameter:
        parameters.append(parameter)
    return parameters


def forbidden_import(source_line: str, feature_name: str, feature_names: set[str]) -> str | None:
    if source_line.startswith(APP_IMPORT):
        return "imports application composition/navigation"
    if not source_line.startswith(FEATURE_IMPORT):
        return None

    imported_feature = source_line.removeprefix(FEATURE_IMPORT).split(".", 1)[0]
    if imported_feature in feature_names and imported_feature != feature_name:
        return f"imports feature '{imported_feature}'"
    return None


def verify() -> list[str]:
    violations: list[str] = []
    feature_dirs = [path for path in FEATURES_ROOT.iterdir() if path.is_dir()]
    feature_names = {path.name for path in feature_dirs}

    for feature_dir in feature_dirs:
        for source in feature_dir.rglob("*.kt"):
            for line in source.read_text(encoding="utf-8").splitlines():
                violation = forbidden_import(line, feature_dir.name, feature_names)
                if violation is not None:
                    violations.append(f"{source.relative_to(ROOT)} {violation}")

        for screen_file in feature_dir.glob("*Screen.kt"):
            source = screen_file.read_text(encoding="utf-8")
            match = SCREEN_SIGNATURE.search(source)
            if match is None:
                violations.append(
                    f"{screen_file.relative_to(ROOT)} has no parseable Screen signature"
                )
                continue

            parameters = split_top_level_parameters(match.group(1))
            if (
                len(parameters) != 2
                or not parameters[0].startswith("state:")
                or not parameters[1].startswith("dispatch:")
            ):
                violations.append(
                    f"{screen_file.relative_to(ROOT)} must accept exactly state + dispatch"
                )

    return violations


def main() -> int:
    violations = verify()
    if violations:
        print("Feature architecture violations:")
        for violation in violations:
            print(f"- {violation}")
        return 1
    print("Feature architecture boundaries verified")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
