# Closed-alpha candidate identity

Every release/physical evidence run must identify the exact software and compatibility contracts under test. This metadata is provenance only; it does not prove that a physical scenario passed.

## Version source of truth

`iosApp/Configuration/Version.xcconfig` is the canonical checked-in application version source:

```text
APP_VERSION = 0.1.0
APP_BUILD = 1
```

Both platform packages consume these values:

- Android Gradle maps them to `versionName` and `versionCode`.
- iOS Xcode maps them to `CFBundleShortVersionString` and `CFBundleVersion` through the debug/release xcconfig files.

`APP_BUILD` must increase for every candidate uploaded to a store channel. Version/build changes are reviewed source changes; release jobs must not silently synthesize a different installed identity.

## Verify repository identity wiring

Run from a clean checkout:

```sh
python3 scripts/release_candidate_identity.py verify
```

The canonical Android `mise run ci` gate runs this verification before tests/assembly.

Verification fails closed when version wiring is inconsistent, required compatibility constants cannot be derived, MediaPipe iOS artifacts do not share one version, model/match-policy identity is malformed, or the working tree is dirty.

## Render evidence metadata

For a candidate that will be installed on physical devices:

```sh
python3 scripts/release_candidate_identity.py render --output /tmp/eyespie-candidate.json
```

The current manifest is **candidate identity schema v2**. Schema v2 adds the application-owned match threshold as required provenance. Schema-v1 manifests are intentionally rejected by the current physical-evidence validator instead of being silently interpreted under the stronger contract.

The generated JSON records only bounded provenance metadata:

- exact Git commit;
- application version/build;
- Android minimum/target SDK and iOS deployment target;
- current SQLDelight schema version derived from migrations;
- current application `MatchEngine.DEFAULT_THRESHOLD` cosine threshold;
- `.eyespie` schema/canonicalization/signature/match-policy versions;
- image-embedding contract, dimension, model ID/file/SHA-256;
- Android MediaPipe Tasks versions;
- project-specific iOS MediaPipe artifact version.

The match threshold is read directly from the application-owned `MatchEngine` source rather than duplicated in release tooling. Physical calibration reports must record this candidate-bound value; the host comparator rejects a different threshold rather than silently comparing/tuning against it.

It intentionally excludes private keys, signatures/payloads, images, embeddings, clues/answers, file paths, tokens, account data, and environment dumps.

## Clean-tree rule

Release evidence must come from an exact clean commit. `render` and `verify` reject a dirty working tree by default.

`--allow-dirty` exists only for local diagnostics while developing the tooling. Output produced with that option is not acceptable release evidence.

## Evidence use

Attach or copy the manifest alongside evidence for:

- #91 physical embedding parity/stability;
- #92 Android ↔ iOS complete game proof;
- #125 MediaPipe/runtime network observation;
- #93 signed internal distribution and install/upgrade evidence.

The installed Android/iOS builds must be produced from the manifest's exact commit and use its declared application build number. If code, runtime/model artifacts, database migrations, bundle compatibility constants, match policy, or version/build identity change, render a new manifest and repeat the affected evidence.
