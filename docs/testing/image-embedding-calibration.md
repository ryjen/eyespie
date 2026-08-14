# Image embedding physical-device calibration

Issue #91 requires real Android and iOS evidence before cross-platform embedding behavior is accepted. Simulator compilation and host tests are useful preconditions, but they are not physical-device proof.

This harness deliberately separates **evidence collection** from **product policy**:

- both platforms use `ImageEmbeddingContract` schema version 1 and the pinned `mobilenet-v3-small-100-224-embedder` model;
- fixtures are immutable MediaPipe testdata objects pinned by GCS generation and SHA-256 in `calibration/image-embedding-fixtures.json`;
- Android and iOS collectors invoke the production `MediaPipeImageEmbeddingGenerator` adapters;
- each fixture is inferred five times, retaining one canonical 1024-float vector plus repeated-inference stability metrics;
- `scripts/compare_image_embedding_calibration.py` reports cosine similarity, RMSE, maximum component delta, and within-platform related/unrelated behavior;
- the comparator contains **no product match threshold** and does not modify schema, normalization, or matching policy.

## Fixture set

| ID | Role | Purpose |
|---|---|---|
| `burger` | reference | exact same source image on both platforms |
| `burger_crop` | related | crop/scale robustness |
| `burger_rotated` | related | orientation robustness |
| `cat` | unrelated | semantic separation control |

The stager rejects unpinned URLs, unexpected hosts/paths, duplicate identities, and SHA-256 mismatches before fixture bytes reach either platform.

## Android physical device

Use a real device selected by `adb`, not an emulator.

```bash
python3 scripts/stage_image_embedding_fixtures.py stage --target android
python3 scripts/stage_image_embedder_model.py stage --target android

./gradlew :app:connectedDebugAndroidTest \
  -Pandroid.testInstrumentationRunnerArguments.class=com.micrantha.eyespie.features.scan.calibration.PhysicalImageEmbeddingCalibrationTest
```

The instrumentation test writes the report into the debug application's private files directory. Export it while the debug application is installed:

```bash
mkdir -p calibration/results
adb exec-out run-as com.micrantha.eyespie.debug \
  cat files/image-embedding-calibration/android.json \
  > calibration/results/android.json

python3 scripts/compare_image_embedding_calibration.py validate \
  calibration/results/android.json
```

Record the physical device model/OS from the report alongside the PR/issue evidence.

## iOS physical device

Fixture packaging is opt-in so ordinary `pod install` and debug builds do not download or package calibration images.

```bash
cd iosApp
EYESPIE_IMAGE_EMBEDDING_CALIBRATION=1 pod install
```

Then, in the `iosApp` Run scheme in Xcode:

1. select a physical iPhone/iPad;
2. add the environment variable `EYESPIE_IMAGE_EMBEDDING_CALIBRATION=1`;
3. run the Debug app and wait for the console line beginning with `EYESPIE_IMAGE_EMBEDDING_CALIBRATION=`;
4. export the app container from Xcode's Devices and Simulators window;
5. copy `Documents/image-embedding-calibration-ios.json` from the container to `calibration/results/ios.json`.

Validate it from the repository root:

```bash
python3 scripts/compare_image_embedding_calibration.py validate \
  calibration/results/ios.json
```

The iOS collector is inert unless the environment variable is explicitly set by the wrapper. Calibration fixtures are only declared as a Debug CocoaPods resource when that same variable is enabled during `pod install`.

## Compare Android and iOS

After collecting reports from both physical devices:

```bash
python3 scripts/compare_image_embedding_calibration.py compare \
  calibration/results/android.json \
  calibration/results/ios.json \
  --json-output calibration/results/comparison.json \
  --markdown-output calibration/results/comparison.md
```

Review at least:

- same-fixture Android/iOS cosine similarity, RMSE, and max absolute component delta;
- repeated-inference minimum cosine and max component delta on each device;
- `burger` → `burger_crop` and `burger` → `burger_rotated` cosine behavior;
- `burger` → `cat` as the unrelated control;
- whether both platforms lead to equivalent product-level match decisions under the **existing** match policy.

Do not infer a new production threshold from one device pair. If evidence suggests a threshold change, treat that as a separate explicit policy change with its own fixture population, device coverage, rationale, and regression tests.

## Evidence gate for #91

The slice is physically accepted only when:

- one real Android report and one real iOS report validate successfully;
- both reports identify the same model SHA-256 and 1024-dimensional schema;
- repeated inference is operationally stable enough to support matching;
- transformed fixtures preserve useful semantic similarity relative to the unrelated control on both platforms;
- the existing product match decision is consistent across platforms for the reviewed fixture set;
- the two raw reports and generated comparison are attached or linked from #91/its implementation PR.

CI may validate provenance, scripts, adapter compilation, and the Android instrumentation APK. CI must not be cited as satisfying the physical-device evidence items above.
