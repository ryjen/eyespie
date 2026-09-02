# Wayfinder deterministic visual-reference plan

Parent: #285.

The runtime restyle in #286 deliberately does not introduce a screenshot library or image-diff dependency into the release candidate without first defining a stable evidence contract. This note bounds the follow-up implementation.

## Required reference states

Capture deterministic Android references for at least:

- Home / empty;
- Home / populated game list;
- Home / verified import preview;
- Onboarding / Local;
- Game detail / creator with clues;
- Play / searching;
- Play / clue found;
- Play / case complete;
- Utility / profile and privacy.

Camera-backed states must use a deterministic fake/static preview surface. Do not require physical camera access or MediaPipe execution merely to render presentation references.

## Determinism contract

Reference rendering must pin:

- viewport/device dimensions;
- density;
- light theme for the first baseline;
- locale (`en` initially);
- font scale (`1.0` baseline plus existing enlarged-font interaction coverage separately);
- fake state/data and target thumbnails;
- animation clock or disabled animations;
- no wall-clock, random, network, filesystem, keychain, camera, or model dependencies.

## Comparison policy

Prefer a maintained Compose screenshot/golden mechanism compatible with the current KMP/Android toolchain. Do not write a custom image comparator unless existing tooling cannot satisfy the contract.

Start with representative screens and reusable visual primitives. Use bounded tolerance for platform text rasterization, but fail obvious changes to:

- palette roles;
- spacing and panel geometry;
- hierarchy/visibility;
- status/action treatment;
- missing/extra major visual elements.

Do not make screenshot approval capable of weakening interaction, accessibility, authority, privacy, or camera lifecycle tests.

## CI boundary

Golden/reference validation belongs beside hosted presentation CI, not physical MediaPipe calibration. A presentation-only reference failure must not require staging the image-embedder model.

Reference-update changes must be reviewable as explicit image diffs and should identify the mockup/visual-contract reason for the change.
