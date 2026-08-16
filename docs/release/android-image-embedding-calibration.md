# Android physical image-embedding calibration

Status: physical-evidence collection procedure for #91/#192. Compiling or running this collector in CI/emulation is **not** physical-device evidence.

The collector uses the same Android `MediaPipeImageEmbeddingGenerator` and packaged image-embedding model loader as normal Eyespie gameplay. It accepts only the reviewed, generation/SHA-pinned non-sensitive calibration fixtures.

## 1. Start from the exact candidate

Use a clean checkout of the exact commit that will be installed/tested. Render candidate identity **before** staging generated fixture assets:

```sh
python3 scripts/release_candidate_identity.py render \
  --output /tmp/eyespie-candidate.json
```

The manifest must be candidate-identity schema v2 and records the source SHA, app version/build, MediaPipe version, active model identity/digest, embedding contract, and production match threshold.

## 2. Stage controlled fixtures

```sh
python3 scripts/stage_image_embedding_fixtures.py stage --target android
python3 scripts/stage_image_embedding_fixtures.py verify --target android
```

The stager validates the checked-in provenance manifest, generation-pinned HTTPS sources, and SHA-256 values before writing ignored test assets under:

`eyespie/src/androidInstrumentedTest/assets/image-embedding-calibration/`

A bounded runtime `manifest.json` containing only fixture ID/role/filename/SHA-256 is generated beside the images. The collector re-hashes every image on-device before inference.

## 3. Connect a representative physical Android device

Confirm the intended device is the target before running Gradle:

```sh
adb devices -l
```

Record the device model and Android version in the #91 evidence notes. The report itself records `Build.MANUFACTURER`, model/device/hardware identifiers, Android release, and SDK level.

## 4. Run the physical collector

```sh
./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.micrantha.eyespie.imaging.calibration.PhysicalImageEmbeddingCalibrationTest \
  --no-daemon --stacktrace
```

The test:

- loads the normal app-packaged `.tflite` through the production digest-verifying model loader;
- verifies its SHA-256 against the active application contract;
- verifies each staged fixture SHA-256 again on-device;
- feeds `CapturedImage.fromEncoded(...)` into the production `MediaPipeImageEmbeddingGenerator`;
- runs exactly five inferences per fixture;
- records first-vector/min-repeat-cosine/max-repeat-delta using the common schema-v2 summarizer;
- records installed app version/build, Android MediaPipe Tasks Vision version, production `MatchEngine.DEFAULT_THRESHOLD`, and device provenance;
- writes only the controlled evidence report to app-private storage.

No camera, gallery, broad storage permission, backend, account, or network connection is required for inference once the APK/test fixture payloads are installed.

## 5. Export the app-private report explicitly

For the debug package:

```sh
adb shell run-as com.micrantha.eyespie.debug \
  cat files/image-embedding-calibration/android.json \
  > /tmp/eyespie-android-calibration.json
```

Do not replace this with broad external-storage output. The report stays app-private until the tester explicitly exports it.

## 6. Validate against the exact candidate

```sh
python3 scripts/compare_image_embedding_calibration.py validate \
  /tmp/eyespie-android-calibration.json \
  --candidate-identity /tmp/eyespie-candidate.json
```

Validation fails closed when app version/build, Android MediaPipe version, model ID/digest, embedding contract, production threshold, fixture identity/SHA, vector shape/finiteness, or repeat count differs from the candidate/evidence contract.

After the matching iOS report from #193 exists, compare both reports with the #92 runbook command.

## Evidence boundary

Acceptable report contents are limited to controlled fixture embeddings and bounded provenance. Do not modify the collector into a generic export surface for user camera images or user-scene embeddings.

Do not attach a report as #91 physical evidence unless:

- it came from the intended representative physical device;
- `/tmp/eyespie-candidate.json` came from the same exact clean source commit used to build/install the test;
- the host validator accepts it;
- device/session notes identify the physical test context without exposing private paths, keys, clues/answers, bundle contents, or unrelated user data.
