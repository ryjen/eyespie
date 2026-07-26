# CI and Fastlane contract

Issue: #62  
Parent: #61

## Ownership boundary

Fastlane is the canonical entry point for application validation, compilation, testing, packaging, and distribution. GitHub Actions remains authoritative for event trust, permissions, runner selection, protected environments, secret injection, concurrency, job dependencies, caching/bootstrap, and artifact publication.

Substantial validation logic remains in directly testable repository scripts. Fastlane invokes those scripts and propagates failures rather than reimplementing them in Ruby.

## Supported branch model

`main` is the sole supported integration branch and pull-request target.

Feature branches receive canonical verification by opening a pull request to `main`. The repository does not maintain a parallel `develop` integration branch or feature-to-feature verification workflow. Staging and production remain explicit environment-deployment paths and do not replace pull-request verification.

## Canonical verification commands

All commands use the committed `Gemfile.lock`:

```bash
bundle exec fastlane android validate
bundle exec fastlane android test
bundle exec fastlane android bundle_debug
bundle exec fastlane ios verify
```

Android build/test lanes preserve an existing `.env.local` and copy `env.example` only when the file is absent. Verification therefore works from a clean checkout without overwriting caller-provided configuration.

### Android validation

`android validate` invokes:

```bash
python3 scripts/stage_android_model_artifact.py verify
python3 -m unittest scripts.tests.test_validate_android_bundle
```

It is platform-neutral and does not require signing or distribution credentials.

The `validate` GitHub Actions job parses the locked Fastlane configuration and invokes this lane:

```bash
bundle exec fastlane lanes
bundle exec fastlane android validate
```

This preserves the required `validate` check name while ensuring the canonical Fastlane contract itself is exercised.

### Android tests

`android test` invokes the unit and snapshot-test contract:

```bash
./gradlew \
  :bluebell:testDebugUnitTest \
  :app:testDebugUnitTest \
  --no-daemon \
  --parallel \
  --build-cache \
  --configuration-cache
```

The `android-tests` GitHub Actions job invokes this lane and remains responsible for test diagnostics and bounded artifact retention.

### Android bundle verification

`android bundle_debug` invokes `scripts/ci/verify_android_bundle.sh`, which:

1. builds `:app:bundleDebug`;
2. requires exactly one generated debug AAB;
3. downloads bundletool 1.18.3 only when the pinned local copy is missing or invalid;
4. verifies the bundletool SHA-256 digest;
5. extracts the `model_pack` manifest;
6. invokes `scripts/validate_android_bundle.py`;
7. writes the topology report under `build/reports/android-bundle`.

The `android-bundle` GitHub Actions job invokes this lane and publishes the topology summary, topology artifact, diagnostics, and generated AAB.

### iOS verification

`ios verify` performs an unsigned simulator build:

```bash
xcodebuild \
  -workspace iosApp/iosApp.xcworkspace \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination "generic/platform=iOS Simulator" \
  CODE_SIGNING_ALLOWED=NO \
  build
```

This is intentionally named `verify`, not `test`, because the current contract proves compilation/linkage rather than executing an iOS test suite. The lane does not initialize Xcode-version management, App Store Connect authentication, provisioning profiles, or other distribution credentials.

## Workflow inventory

| Workflow | Current triggers | Classification | Runner / secrets | Current build interface | Disposition |
|---|---|---|---|---|---|
| `main.yml` | PRs and pushes to `main` | Active verification | Linux; no protected distribution environment | Canonical Fastlane validation, test, and bundle lanes | Retain as the required PR/post-merge verification path |
| `staging.yml` | `integration/**` pushes; callable/manual | Environment deployment | macOS plus staging secrets; Ubuntu Supabase deployment | Fastlane distribution plus composite Supabase action | Preserve until shared deployment orchestration slice |
| `production.yml` | pushes to `main`; callable/manual | Environment deployment | macOS plus production secrets; Ubuntu Supabase deployment | Fastlane distribution plus composite Supabase action | Later slice: gate on successful same-commit verification and share deployment setup |
| `sbom.yml` | PRs/pushes to `main`, tags, manual | Independent supply-chain workflow | Linux classifier and macOS generation; release permissions where applicable | Direct Gradle, CocoaPods and repository validation | Retain GitHub-specific publication/attestation; reconcile preparation later |
| `workflow-security.yml` | workflow/action/dependabot changes; manual | Independent security workflow | Linux; read-only token | Zizmor | Retain independently; not application build semantics |
| `codex-code-review.yml.disabled` | Disabled | Inactive | None | None | Keep disabled or delete separately; out of scope for application CI consolidation |

Removed obsolete workflows:

- `test.yml`: duplicated verification for `feature/**` branch flows on macOS and required the protected `test` environment.
- `development.yml`: duplicated verification for the unsupported `develop` branch, required protected development configuration, and contained a permanently disabled Supabase job.

## Security invariants

- Verification lanes do not read signing, Play Store, App Store Connect, Supabase, or production application secrets.
- Ordinary pull-request verification does not require a protected environment.
- Distribution authentication remains confined to distribution lanes and protected GitHub environments.
- Existing clean/Xcode-selection/artifact-cleanup hooks remain active for non-verification lanes but are excluded from verification lanes.
- GitHub Actions remains responsible for rejecting unsafe event paths and controlling secret availability.
- Checkout credentials remain disabled unless a narrowly scoped write operation requires them.
- Downloaded tools are pinned and checksum-verified.

## Follow-up slices

1. Extract shared staging/production deployment orchestration.
2. Gate production on successful verification of the same commit.
3. Reconcile SBOM preparation and record before/after CI performance measurements.
