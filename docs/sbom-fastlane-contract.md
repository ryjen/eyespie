# SBOM Fastlane preparation contract

Issue: #72  
Parent: #61

## Canonical commands

```bash
bundle exec fastlane sbom_android
bundle exec fastlane sbom_ios_prepare
```

## Android SBOM preparation

`sbom_android`:

- preserves an existing `.env.local`;
- copies `env.example` only when `.env.local` is absent;
- invokes `:app:cyclonedxDirectBom`;
- uses the existing no-daemon, parallel, build-cache, and configuration-cache settings;
- leaves `build/reports/sbom/eyespie-gradle.cdx.json` available to the caller.

## CocoaPods SBOM preparation

`sbom_ios_prepare`:

- preserves an existing `.env.local`;
- generates the application podspec and dummy framework;
- runs `pod install --deployment` against `iosApp`;
- fails when the committed lockfile cannot be reproduced;
- leaves generated podspec, lockfile, and dependency graph available to the caller.

## Security and lifecycle boundary

These lanes are top-level Fastlane lanes rather than Android or iOS platform lanes. They therefore do not enter platform build/distribution hooks that initialize Xcode version management, run global clean operations, or remove generated artifacts.

The lanes do not read signing, App Store Connect, Play Store, Supabase, staging, or production credentials. GitHub Actions remains responsible for event classification, permissions, release publication, attestations, artifact retention, and the stable terminal `generate` check.

## Follow-up

The next sub-slice of #72 will migrate `.github/workflows/sbom.yml` to these commands and record before/after workflow timing evidence without changing SBOM content, publication, or branch-protection semantics.
