# SBOM continuous integration

EyesPie generates CycloneDX SBOMs for Gradle/Kotlin Multiplatform and CocoaPods dependencies.

## Pull requests

The SBOM workflow first classifies changed files on an Ubuntu runner. Pull requests that modify dependency, build, packaging, model-pack, CocoaPods, or SBOM inputs run the full macOS generation job. Documentation-only and other unrelated pull requests skip that worker.

The terminal `generate` job always completes, including when generation is intentionally skipped, so branch protection does not remain pending.

Pull-request SBOM artifacts are retained for 7 days.

## Trusted runs

Pushes to `main`, version tags, and manually dispatched workflows always run full SBOM generation. Their generated SBOM bundles are retained for 30 days.

## Fail-safe inputs

Changes to the SBOM workflow itself, SBOM-related scripts, Gradle build and wrapper inputs, CocoaPods metadata, or `model-pack/` force full generation.
