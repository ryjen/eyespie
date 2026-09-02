# Wayfinder deterministic visual-reference contract

Parent: #285. Implementation: #287 / PR #290. Remaining visual convergence: #291.

The Wayfinder alpha surface now has checked-in deterministic Android references backed by Roborazzi. These references make visual changes reviewable and prevent silent regressions, but they do **not** by themselves prove that the current composition fully matches the exploratory canonical board. The first exact side-by-side review identified remaining convergence work in #291.

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

Camera-backed states use `LocalCameraCaptureSurfaceOverride`, a presentation-only `CompositionLocal` whose production default remains the real platform `CameraCapture`. The fake surface supplies only a fixed field background/focus mark and invokes the same production overlay content.

The existing hosted enlarged-font interaction suite remains separate. Golden references must not be made stable by disabling the app's production dynamic-type behavior.

## Checked-in reference states

| Reference | Contract exercised |
| --- | --- |
| `home_empty.png` | field-desk identity, Local Mode, create/import, empty games state |
| `home_populated.png` | local/shared game rows, progress/badges, game-list hierarchy |
| `verified_import_preview.png` | verified signed-file preview and confirm/cancel hierarchy |
| `onboarding_local.png` | Local onboarding content, illustration, progress/actions |
| `game_detail_creator.png` | creator case detail, progress, authoring/share/clue hierarchy |
| `play_searching.png` | camera field, case/clue overlay, primary capture action |
| `play_clue_found.png` | explicit successful-match feedback and next-clue action |
| `play_case_complete.png` | terminal completion hierarchy |
| `utility_profile_privacy.png` | local identity, privacy/sharing and help/settings treatment |

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

## First board comparison

The first checked-in reference set was compared directly with all eight tiles under `docs/design/eyespie-app-mockups/`. It is a valid deterministic **current-state baseline** and clearly reflects the Micrantha/travel-spy language introduced in #286. It also makes several remaining composition gaps obvious; those are recorded in `wayfinder-visual-review.md` and tracked by #291.

Therefore:

- #287 can close when PR #290's verify-mode CI is accepted;
- #285 remains open through #291 and the final representative installed-build visual review.
