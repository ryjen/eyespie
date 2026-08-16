# iOS physical image-embedding calibration

Status: physical-evidence collection procedure for #91/#193. Compiling this collector in CI or running the normal simulator application is **not** physical-device evidence.

The collector uses the same iOS `MediaPipeImageEmbeddingGenerator` and digest-verified packaged `.tflite` model as normal Eyespie gameplay. Calibration fixture resources are a controlled Debug-only test overlay and are not present in ordinary app builds.

## 1. Start from the exact clean candidate

From the exact source commit intended for the physical test, render candidate identity **before** staging generated fixture assets:

```sh
python3 scripts/release_candidate_identity.py render \
  --output /tmp/eyespie-candidate.json
```

Candidate-identity schema v2 binds the source SHA, app version/build, project-specific iOS MediaPipe artifact version, model ID/SHA-256, embedding contract, and production match threshold.

## 2. Stage the controlled fixtures

```sh
python3 scripts/stage_image_embedding_fixtures.py stage --target ios
python3 scripts/stage_image_embedding_fixtures.py verify --target ios
```

The generated `*.jpg` files and bounded runtime `manifest.json` are ignored by Git. The stager verifies the checked-in source revision/generation/SHA provenance before writing them.

The controlled fixture overlay changes the Debug test bundle contents but not application gameplay authority, model/runtime code, source SHA, or candidate compatibility identity. Do not use this Debug calibration bundle as the final store-distribution artifact.

## 3. Install pods with calibration explicitly enabled

Ordinary `pod install` omits the fixture pod. The calibration pod can be enabled only for a Debug install:

```sh
cd iosApp
CONFIGURATION=Debug EYESPIE_IMAGE_EMBEDDING_CALIBRATION=1 pod install --repo-update
cd ..
```

The Podfile refuses calibration packaging outside Debug configuration. The opt-in fixture pod packages only the four pinned JPEGs and their bounded runtime manifest.

## 4. Build/install on a representative physical iPhone

Open `iosApp/iosApp.xcworkspace` in Xcode, select the intended physical iPhone and the Debug configuration, and confirm the installed app reports the same version/build as `/tmp/eyespie-candidate.json`.

For the Run action, set this scheme environment variable:

```text
EYESPIE_IMAGE_EMBEDDING_CALIBRATION=1
```

The trigger exists only inside `#if DEBUG`. Release builds contain no automatic calibration launch behavior.

On launch the collector:

- loads the normal packaged model through the production digest-verifying resolver;
- reads `EyespieMediaPipeTasksVersion` from the installed app Info.plist, whose value comes from the same `MediaPipe.xcconfig` used by KMP CocoaPods dependencies;
- strictly parses the stager-generated bounded fixture manifest;
- re-hashes every fixture on-device before inference;
- feeds `CapturedImage.fromEncoded(...)` into the production `MediaPipeImageEmbeddingGenerator`;
- runs exactly five inferences per fixture;
- records the first vector, minimum repeat cosine, and maximum repeat delta through the common schema-v2 summarizer;
- records installed app version/build, Apple hardware identifier/iOS version, project-specific MediaPipe version, model digest, and production `MatchEngine.DEFAULT_THRESHOLD`;
- emits no user camera image, user-scene embedding, clue/answer, bundle payload, private key, token, or arbitrary environment value.

The wrapper logs only one stable marker:

```text
EYESPIE_IMAGE_EMBEDDING_CALIBRATION=complete
```

or

```text
EYESPIE_IMAGE_EMBEDDING_CALIBRATION=failed
```

It does not print the app-container path or exception payload.

## 5. Export the report explicitly

The fixed report filename is:

```text
Documents/image-embedding-calibration-ios.json
```

Use Xcode's **Devices and Simulators** window to download the app container for the physical device, then copy that fixed Documents file to:

```text
/tmp/eyespie-ios-calibration.json
```

The app does not write the report to shared Photos/media storage or upload it anywhere.

## 6. Validate against the exact candidate

```sh
python3 scripts/compare_image_embedding_calibration.py validate \
  /tmp/eyespie-ios-calibration.json \
  --candidate-identity /tmp/eyespie-candidate.json
```

Validation fails closed when app version/build, iOS MediaPipe artifact version, model ID/digest, embedding contract, candidate-bound production threshold, fixture identity/SHA, vector shape/finiteness, or repeat count differs.

After the physical Android report exists:

```sh
python3 scripts/compare_image_embedding_calibration.py compare \
  /tmp/eyespie-android-calibration.json \
  /tmp/eyespie-ios-calibration.json \
  --candidate-identity /tmp/eyespie-candidate.json \
  --json-output calibration/results/cross-platform.json \
  --markdown-output calibration/results/cross-platform.md
```

The comparator evaluates the threshold already bound into the candidate manifest; it never tunes or selects a replacement threshold.

## 7. Restore the normal dependency graph

After evidence collection, remove the Debug calibration overlay by running ordinary pod installation without the opt-in environment variable:

```sh
cd iosApp
pod install --repo-update
cd ..
```

Do not ship a store candidate from a workspace whose intended distribution dependency graph has not been restored/revalidated.

## Evidence boundary

Do not attach an iOS report as #91 physical evidence unless:

- it came from the intended representative physical iPhone;
- the candidate manifest came from the same exact clean source commit used to build the test app;
- the host validator accepts the report;
- the controlled Debug fixture overlay and physical device/OS are identified in evidence notes;
- no private container paths, keys, clues/answers, bundle contents, unrelated user traffic, or arbitrary user-scene embeddings are included.
