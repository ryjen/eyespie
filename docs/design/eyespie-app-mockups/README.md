# Eyespie app mockups

Whole-app UX concept for [#220](https://github.com/ryjen/eyespie/issues/220), captured 2026-08-21.

These are exploratory design mockups for discussion and iteration, not a pixel-exact implementation contract.

The original board is stored as eight review-friendly tiles preserving the full layout:

| Left | Right |
|---|---|
| ![Board row 1 left](01.webp) | ![Board row 1 right](02.webp) |
| ![Board row 2 left](03.webp) | ![Board row 2 right](04.webp) |
| ![Board row 3 left](05.webp) | ![Board row 3 right](06.webp) |
| ![Board row 4 left](07.webp) | ![Board row 4 right](08.webp) |

## Scope

The board covers:

- local-mode onboarding;
- game list, creation, clue authoring, and game details;
- signed `.eyespie` share/export;
- import selection, preview, success, conflict, and invalid-file states;
- camera/play mode and clue-found feedback;
- game progress;
- profile/settings and re-openable onboarding.

## Notes

- Local-only/backendless remains the product model.
- `.eyespie` is the signed portable game format.
- Treat this board as a UX direction for #220, not a final component specification.
- The tiles are lightly compressed for repository review while preserving the complete board composition.
