# MediaPipe iOS Distribution Contract

## Purpose

Eyespie uses a project-specific MediaPipe iOS distribution because the required Common, Vision, GenAIC, and GenAI components must be built as one coherent family from one source revision and toolchain.

This document defines the consumption, validation, capability, and rollback contract for that distribution.

## Current release

- Repository: `ryjen/mediapipe`
- Tag: `eyespie-ios-v0.10.26.1`
- Status: prerelease until the complete Eyespie KMP and application integration gate passes
- Required pods:
  - `EyespieMediaPipeTasksCommon` `0.10.26.1`
  - `EyespieMediaPipeTasksVision` `0.10.26.1`
  - `EyespieMediaPipeTasksGenAIC` `0.10.26.1`
  - `EyespieMediaPipeTasksGenAI` `0.10.26.1`

The namespaced pod names prevent accidental resolution against unrelated CocoaPods trunk packages. The framework module names remain compatible with the official MediaPipe imports.

## Consumption rules

- Resolve only immutable checked-in podspecs or URLs pinned to the release tag.
- Do not consume a branch, `latest` URL, or mutable generated artifact.
- Declare the same four pods and exact version in both the Kotlin CocoaPods configuration and native Podfile.
- Use explicit static linkage.
- Do not mix official `MediaPipeTasks*` pods with the namespaced Eyespie distribution.
- Commit the generated lockfile only after a clean installation succeeds.
- Treat the lockfile, podspecs, archive checksums, and provenance as one release contract.

## Required verification

A candidate is accepted only when a clean macOS runner proves:

1. All release assets exist and match the published checksums.
2. CocoaPods installs exactly the four namespaced pods at `0.10.26.1`.
3. No official or mixed-version MediaPipe pod is present in the lockfile.
4. Kotlin/Native cinterop compiles for simulator arm64 and x86_64 where supported.
5. The static Eyespie KMP framework links.
6. The real `iosApp.xcworkspace` builds for an unsigned simulator target.
7. The final app contains the expected linked frameworks and no unresolved MediaPipe, protobuf, SQLite, C++, or runtime symbols.
8. Eyespie constructs the MediaPipe Vision task used by the product at runtime.
9. The CocoaPods SBOM path reproduces the same locked graph.

Successful archive creation or standalone CocoaPods consumer tests are necessary but not sufficient.

## Capability boundary

The project-specific GenAI iOS build is CPU-only.

The public-source implementation does not provide working image-input GenAI. Eyespie must therefore:

- report image-input GenAI as unsupported on iOS;
- avoid attempting the unavailable API;
- avoid presenting the capability in onboarding, settings, marketing, or release notes;
- retain supported Vision functionality independently;
- keep local text-only GenAI experimental until runtime and performance validation are complete.

## Supply-chain requirements

- Record the upstream MediaPipe revision and all compatibility patches.
- Pin the build toolchain and executable GitHub Actions dependencies.
- Publish SHA-256 checksums and provenance beside every archive and podspec.
- Preserve a bounded SBOM entry for the selected release components.
- Review licenses and redistribution terms before promoting the prerelease.
- Never execute or load an artifact that failed checksum or compatibility verification.

## Diagnostics

On failure, CI should retain bounded evidence sufficient to identify the first causal error:

- CocoaPods command output;
- selected podspec paths and URLs;
- generated podspec and Podfile;
- lockfile or partial resolution state;
- relevant xcconfig and linker command fragments;
- selected architecture slices and deployment targets;
- unresolved `mediapipe::`, `google::protobuf::`, SQLite, or C++ symbols.

Diagnostics must not include credentials, signing material, environment dumps, private local paths beyond repository-relative context, or protected application configuration.

## Promotion policy

The `eyespie-ios-v0.10.26.1` release remains a prerelease until:

- the Eyespie integration PR is green;
- the locked CocoaPods and SBOM paths agree;
- Vision runtime construction succeeds;
- the application is exercised on a physical iOS device.

Promotion does not mean the artifacts become general-purpose upstream replacements. They remain a project-specific distribution with a documented compatibility and support boundary.

## Rollback

Rollback restores the entire known-good family, not individual pods.

Restore together:

- all four pod versions;
- checked-in podspecs;
- Podfile declarations;
- generated Kotlin podspec metadata;
- Podfile.lock;
- checksums and provenance references;
- supported architecture and deployment-target policy.

Trigger rollback on mixed resolution, checksum mismatch, unsupported slices, unresolved native symbols, runtime construction failure, or nondeterministic clean installation.
