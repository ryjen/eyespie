# Wayfinder deterministic visual-reference contract

Parent: #285. Initial implementation: #287 / PR #290. Composition convergence: #291 / PR #292. Authoring capture/review correction: #293 / PR #294.

The Wayfinder alpha surface has checked-in deterministic Android references backed by Roborazzi. These references make visual changes reviewable and prevent silent regressions, but they do **not** by themselves prove that the current composition fully matches the exploratory canonical board. Human comparison against the board and the documented authority/accessibility contracts remains required.

## Toolchain

- Roborazzi `1.72.0` (already pinned in the repository version catalog);
- Robolectric `4.14.1`;
- Android API 35;
- Compose UI test rule rendering the real common `EyespieTheme` and feature composables;
- reference files under `eyespie/src/androidUnitTest/goldens/wayfinder/`.

No custom image comparator was introduced.

## Determinism contract

The primary reference environment is pinned to:

- Pixel 5 resource dimensions: `393dp × 851dp`;
- `440dpi` density through `RobolectricDeviceQualifiers.Pixel5`;
- Android API 35;
- locale `en-US`;
- font scale `1.0`;
- canonical light `EyespieTheme`;
- fixed fake game/identity/clue/progress values;
- no wall clock or random values;
- no network, filesystem, SQLDelight, keychain, physical camera, model staging, or MediaPipe execution.

Camera-backed states use `LocalCameraCaptureSurfaceOverride`, a presentation-only `CompositionLocal` whose production default remains the real platform `CameraCapture`. The fake surface supplies only a fixed field background/focus mark and invokes the same production overlay content. Authoring review references use a deterministic encoded still so the captured-image phase can be exercised without camera or model authority.

The existing hosted enlarged-font interaction suite remains separate. Golden references must not be made stable by disabling the app's production dynamic-type behavior.

## Checked-in reference states

| Reference | Contract exercised |
| --- | --- |
| `home_empty.png` | field-desk identity, Local Mode, create/import, empty games state |
| `home_populated.png` | local/shared game rows, progress/badges, game-list hierarchy |
| `verified_import_preview.png` | verified signed-file preview and confirm/cancel hierarchy |
| `onboarding_local.png` | Local onboarding content, illustration, progress/actions |
| `game_detail_creator.png` | creator case detail, progress, authoring/share/clue hierarchy |
| `create_live_capture.png` | full camera field, minimal safe-area chrome, shutter, and no pre-capture form |
| `clue_authoring_review.png` | captured-still field with creator form plus retake/commit hierarchy |
| `play_searching.png` | camera field, case/clue overlay, primary capture action |
| `play_clue_found.png` | explicit successful-match feedback and next-clue action |
| `play_case_complete.png` | terminal completion hierarchy |
| `utility_profile_privacy.png` | local identity, privacy/sharing and help/settings treatment |

The two #293 authoring references were generated on the PR head, inspected before recording, and accepted because they encode the intended phase boundary: no form over a live camera; the form appears only over the captured still. Existing Play references were deliberately left unchanged.

## Comparison policy

`WayfinderGoldenTest` uses a Roborazzi comparison threshold of `0.002` (0.2% changed pixels). This allows a very small bounded amount of host rasterization variation while still rejecting material changes in:

- Micrantha palette roles;
- screen hierarchy;
- panel/action geometry;
- spacing and content placement;
- progress/status treatment;
- missing or additional major elements.

Reference recording is an explicit maintenance operation:

```text
./gradlew :app:recordRoborazziDebug
```

Normal validation is verification-only:

```text
./gradlew :app:verifyRoborazziDebug
```

The permanent `Wayfinder visual references` GitHub Actions workflow has read-only repository permissions and runs verification. On failure it uploads Roborazzi reports/diff outputs for review. It does not stage the image-embedder model or require camera/MediaPipe evidence.

## Reference update policy

A golden update is not a substitute for review. Any changed reference must:

1. be inspected as an image diff;
2. identify the visual-contract or canonical-board reason for the change;
3. preserve semantic interaction/accessibility tests;
4. preserve `.eyespie`, local authority, privacy, creator-only and camera lifecycle contracts;
5. avoid approving an unexpected diff merely to make CI green.

## Board comparison and convergence

The first checked-in reference set was compared directly with all eight tiles under `docs/design/eyespie-app-mockups/`. It established a deterministic current-state baseline and exposed the composition gaps subsequently addressed by #291 / PR #292.

#293 / PR #294 supersedes one assumption from that convergence pass for Create Game and Clue Authoring. Those surfaces are now explicitly two-phase:

```text
live camera -> capture -> captured still + authoring form -> retake or commit
```

The captured image remains route-local/transient until commit. The visual reference suite tests the presentation boundary only; interaction tests separately prove that capture does not submit creator authority and that retake/commit dispatch correctly.

Therefore #285 remains presentation-complete only when PR #294's final head is green across deterministic references, Android core, hosted screen instrumentation, and iOS application/runtime validation.
