# CI and Fastlane contract

Issue: #62  
Parent: #61

## Ownership boundary

Fastlane is the canonical entry point for application validation, compilation, testing, packaging, and distribution. GitHub Actions remains authoritative for event trust, permissions, runner selection, protected environments, secret injection, concurrency, job dependencies, caching/bootstrap, and artifact publication.

Substantial validation logic remains in directly testable repository scripts. Fastlane invokes those scripts and propagates failures rather than reimplementing them in Ruby.

## Canonical verification commands

All commands use the committed `Gemfile.lock`:

```bash
bundle exec fastlane android validate
bundle exec fastlane android test
bundle exec fastlane android bundle_debug
bundle exec fastlane ios test
```

### Android validation

`android validate` invokes:

```bash
python3 scripts/stage_android_model_artifact.py verify
python3 -m unittest scripts.tests.test_validate_android_bundle
```

It is platform-neutral, uses checked-in safe configuration, and does not require signing or distribution credentials.

### Android tests

`android test` invokes the existing unit and snapshot-test contract:

```bash
./gradlew \
  :bluebell:testDebugUnitTest \
  :app:testDebugUnitTest \
  --no-daemon \
  --parallel \
  --build-cache \
  --configuration-cache
```

### Android bundle verification

`android bundle_debug` invokes `scripts/ci/verify_android_bundle.sh`, which:

1. builds `:app:bundleDebug`;
2. requires exactly one generated debug AAB;
3. downloads bundletool 1.18.3 only when the pinned local copy is missing or invalid;
4. verifies the bundletool SHA-256 digest;
5. extracts the `model_pack` manifest;
6. invokes `scripts/validate_android_bundle.py`;
7. writes the topology report under `build/reports/android-bundle`.

### iOS verification

`ios test` performs an unsigned simulator build:

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

The lane must not initialize App Store Connect authentication, provisioning profiles, or other distribution credentials.

## Workflow inventory

| Workflow | Current triggers | Classification | Runner / secrets | Current build interface | Proposed disposition |
|---|---|---|---|---|---|
| `main.yml` | PRs and pushes to `main` | Active verification | Linux; no protected distribution environment | Direct Gradle and repository scripts | Later slice: delegate application semantics to the canonical Fastlane lanes while preserving job names and diagnostics |
| `test.yml` | `feature/**` pushes and PRs targeting `feature/**`; callable/manual | Obsolete candidate | macOS; `test` environment config | Top-level `fastlane test` | Confirm branch-flow use, then remove unless feature-to-feature PRs are supported |
| `development.yml` | `develop` pushes/PRs; callable/manual | Obsolete or manual-only candidate | macOS; development environment and app configuration | Top-level `fastlane test`; disabled Supabase job | Confirm whether `develop` remains supported; otherwise remove rather than refactor |
| `staging.yml` | `integration/**` pushes; callable/manual | Environment deployment | macOS plus staging secrets; Ubuntu Supabase deployment | Fastlane distribution plus composite Supabase action | Preserve until shared deployment orchestration slice |
| `production.yml` | pushes to `main`; callable/manual | Environment deployment | macOS plus production secrets; Ubuntu Supabase deployment | Fastlane distribution plus composite Supabase action | Later slice: gate on successful same-commit verification and share deployment setup |
| `sbom.yml` | PRs/pushes to `main`, tags, manual | Independent supply-chain workflow | Linux classifier and macOS generation; release permissions where applicable | Direct Gradle, CocoaPods and repository validation | Retain GitHub-specific publication/attestation; optionally invoke canonical preparation lanes where useful |
| `workflow-security.yml` | workflow/action/dependabot changes; manual | Independent security workflow | Linux; read-only token | Zizmor | Retain independently; not application build semantics |
| `codex-code-review.yml.disabled` | Disabled | Inactive | None | None | Keep disabled or delete separately; out of scope for application CI consolidation |

## Security invariants

- Verification lanes do not read signing, Play Store, App Store Connect, Supabase, or production application secrets.
- Distribution authentication remains confined to distribution lanes and protected GitHub environments.
- GitHub Actions remains responsible for rejecting unsafe event paths and controlling secret availability.
- Checkout credentials remain disabled unless a narrowly scoped write operation requires them.
- Downloaded tools are pinned and checksum-verified.
- Fastlane global hooks must not perform destructive cleans, initialize distribution authentication, or erase diagnostics for verification lanes.

## Follow-up slices

1. Migrate `main.yml` to the canonical Fastlane lanes while preserving required job names.
2. Remove or retain `test.yml` and `development.yml` based on confirmed branch strategy.
3. Extract shared staging/production deployment orchestration.
4. Gate production on successful verification of the same commit.
5. Reconcile SBOM preparation and record before/after CI performance measurements.
