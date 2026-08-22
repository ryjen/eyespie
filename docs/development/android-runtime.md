# Android runtime provisioning

Eyespie's ordinary Android build and CI paths intentionally remain network-independent. The image-embedding model required by the runnable application is therefore provisioned explicitly rather than downloaded as a side effect of `assembleDebug`.

## Gradle project name

The Android/Kotlin Multiplatform application lives in the `eyespie/` directory, but Gradle maps that directory to the `:app` project:

```kotlin
include(":app")
project(":app").projectDir = file("eyespie")
```

Use `:app:` in Gradle task paths. `:eyespie:` is not a Gradle project.

## Runtime lifecycle

Treat provisioning as three distinct operations:

1. **Stage** — may use network access. Downloads the generation-pinned artifact when needed and accepts it only after manifest, byte-size, and SHA-256 validation.
2. **Verify** — strictly offline. Re-reads the staged artifact and proves it still matches the pinned manifest without downloading or repairing anything.
3. **Install/run** — consumes the already-staged artifact. It is not evidence of provenance unless verification has succeeded first.

The explicit commands are:

```bash
./gradlew :app:stageAndroidImageEmbedderModel
./gradlew :app:verifyAndroidRuntime
./gradlew :app:installDebug
```

The verifier calls the existing `scripts/stage_image_embedder_model.py verify --target android` path. It fails closed if the staged model is absent, truncated, stale/same-size-tampered, or digest-mismatched.

## Recommended local workflow

Use the repository-level mise task when provisioning a development device:

```bash
mise run android-runtime
```

This performs stage → offline verify → install in separate Gradle invocations so provisioning completes before verification and installation begin.

To prove readiness without allowing provisioning or downloads, use:

```bash
mise run android-runtime-verify
```

That command is appropriate after staging and before physical-device evidence collection. It is deliberately useful with network access disabled: a missing or invalid artifact fails instead of being repaired implicitly.

To provision only the model:

```bash
./gradlew :app:stageAndroidImageEmbedderModel
```

## Offline build contract

These commands do **not** provision the model and must remain network-independent:

```bash
mise run build
mise run ci
./gradlew :app:assembleDebug
./gradlew :app:verifyAndroidRuntime
mise run android-runtime-verify
```

The ordinary build/CI commands validate the backendless/local-authoritative core and build graph without silently reaching the network. A debug APK produced through that offline build path is not, by itself, proof that all externally staged runtime assets are present.

`verifyAndroidRuntime` is different: it explicitly checks runtime readiness, but it never stages, downloads, or mutates the model artifact.

## Failure diagnosis

If the installed application shows `The local game runtime could not be initialized`, first run:

```bash
mise run android-runtime-verify
```

Typical failures are intentionally specific:

- **missing artifact** — stage the model, then verify again;
- **byte-size mismatch** — treat the staged file as truncated/stale and re-stage from the pinned source;
- **SHA-256 mismatch** — do not run the candidate; re-stage and re-verify;
- **manifest/provenance failure** — fix the checked-in contract rather than bypassing validation.

After readiness verifies, inspect Logcat for the `Eyespie` tag if runtime initialization still fails. Runtime construction failures are logged with their exception and stack trace before the fail-closed `AppUnavailable` UI is rendered.

## Physical/release preparation

For Android candidate-bound evidence, render candidate identity from the clean source commit first, stage the ignored runtime artifact, then execute `mise run android-runtime-verify` before installing or collecting physical evidence. Record the successful verification together with the device/session evidence required by the relevant release issue.

A successful host-side verification proves the staged bytes match the pinned contract. #239 still requires on-device evidence that a verified staged artifact allows `createAndroidEyespieRuntime()` to initialize past the model-loading boundary; host verification is not a substitute for that physical observation.

## Shared automation boundary

The current staging/verification implementation is Eyespie-specific. A reusable Bluebell/community abstraction may eventually own the generic contract for externally provisioned runtime artifacts:

- declarative artifact manifest;
- pinned source and expected size/hash;
- explicit network-enabled staging;
- offline verification;
- platform-specific destination;
- clear build/runtime task integration without implicit downloads.

Until such an abstraction exists and is proven in another consumer, Eyespie should retain the local implementation rather than coupling runtime readiness to an unproven shared plugin API.
