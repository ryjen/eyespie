# Android model artifact staging

The MediaPipe model is intentionally excluded from Git. A valid `.task` bundle must be staged before producing an internal Play build.

## Candidate

The current small-device candidate is the gated `litert-community/gemma-3-270m-it` MediaPipe task bundle on Hugging Face. Access requires accepting the Gemma terms. Do not redistribute it until the release owner has confirmed the applicable terms and included the required notices in the application distribution.

The candidate is not downloaded automatically by CI. This avoids silently accepting model terms, leaking credentials, or treating a mutable remote file as a release identity.

## Stage an approved artifact

```bash
python3 scripts/stage_android_model_artifact.py stage \
  /absolute/path/to/gemma3-270m-it-q8.task \
  --model-id eyespie-offline-model \
  --version gemma3-270m-it-q8-<immutable-release>
```

The command:

1. copies the `.task` file into `model-pack/src/main/assets/model/`;
2. calculates its exact byte size and streaming SHA-256;
3. rewrites `manifest.json`;
4. rewrites `androidSmokeModelDescriptor` with the same immutable identity.

The `.task` file remains ignored by Git. Commit only the generated manifest and descriptor changes.

## Verify before a Play build

```bash
./gradlew :model-pack:verifyModelArtifact
```

This task fails when:

- the staged artifact is missing;
- descriptor and manifest metadata differ;
- the actual byte size differs;
- the SHA-256 differs.

Run it before generating and uploading the Android App Bundle to the internal Play track.

## Source-only CI

Source CI can verify the committed metadata without downloading the gated model:

```bash
python3 scripts/stage_android_model_artifact.py verify
```

This checks descriptor/manifest consistency and reports that the ignored binary is not staged.
