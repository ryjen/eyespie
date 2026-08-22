# Android runtime provisioning

Eyespie's ordinary Android build and CI paths intentionally remain network-independent. The image-embedding model required by the runnable application is therefore provisioned explicitly rather than downloaded as a side effect of `assembleDebug`.

## Gradle project name

The Android/Kotlin Multiplatform application lives in the `eyespie/` directory, but Gradle maps that directory to the `:app` project:

```kotlin
include(":app")
project(":app").projectDir = file("eyespie")
```

Use `:app:` in Gradle task paths. `:eyespie:` is not a Gradle project.

## Recommended local workflow

Use the repository-level mise task:

```bash
mise run android-runtime
```

This performs two explicit steps:

1. `:app:stageAndroidImageEmbedderModel` downloads (when necessary), validates, and stages the generation-pinned image-embedder model using `models/image-embedder.json` and `scripts/stage_image_embedder_model.py`.
2. `:app:installDebug` builds and installs the debug application after the model has been staged.

The staging and install steps are separate Gradle invocations so that model provisioning is complete before the Android build begins.

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
```

They validate the backendless/local-authoritative core and build graph without silently reaching the network. A debug APK produced through the offline build path is not, by itself, proof that all optional externally staged runtime assets are present.

## Failure diagnosis

If the installed application shows `The local game runtime could not be initialized`, inspect Logcat for the `Eyespie` tag. Runtime construction failures are logged with their exception and stack trace before the fail-closed `AppUnavailable` UI is rendered.

The most common provisioning check is:

```bash
mise run android-runtime
```

If the model is already correctly staged, the staging script verifies/reuses the pinned artifact rather than changing the application trust boundary.

## Shared automation boundary

The current staging implementation is Eyespie-specific. A reusable Bluebell/community abstraction may eventually own the generic contract for externally provisioned runtime artifacts:

- declarative artifact manifest;
- pinned source and expected size/hash;
- explicit network-enabled staging;
- offline verification;
- platform-specific destination;
- clear build/runtime task integration without implicit downloads.

Until such an abstraction exists and is proven in another consumer, Eyespie should retain the local implementation rather than coupling runtime readiness to an unproven shared plugin API.
