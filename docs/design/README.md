# Eyespie design

This directory is the canonical home for Eyespie UX and product-design material.

## Current design direction

- [`eyespie-app-mockups/`](eyespie-app-mockups/) — current whole-app local-mode UX mockups and implementation constraints for #220. These supersede the former top-level `design/mockups.png` board.

## Design notes

The following documents were moved from the former top-level `design/` directory into this canonical location:

- [`overview.md`](overview.md)
- [`features.md`](features.md)
- [`workflow.md`](workflow.md)
- [`enhancements.md`](enhancements.md)

Some of their concepts pre-date the backendless/local-authoritative reboot and include account/login, challenge feeds, leaderboards, sync, geo-fencing, seasonal rewards, and other ideas that are **not current alpha architecture or implementation commitments**. Treat them as product/design exploration unless reaffirmed by current architecture, issues, ADRs, or implementation notes.

The old `design/mockups.png` is intentionally not retained because it has been superseded by the current whole-app mockups.

## Documentation boundary

New design artifacts should be added under `docs/design/`; do not recreate a top-level `design/` tree.
