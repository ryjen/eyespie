# Eyespie design

This directory is the canonical home for Eyespie UX and product-design material.

## Current design direction

- [`eyespie-app-mockups/`](eyespie-app-mockups/) — current whole-app local-mode UX mockups and implementation constraints for #220.

## Historical concepts

The former top-level `design/` directory pre-dates the backendless/local-authoritative reboot and is retained under [`legacy/`](legacy/) for product-history reference only.

Those documents include account/login, challenge feeds, leaderboards, sync, geo-fencing, seasonal rewards, and other concepts that are **not current alpha architecture or implementation commitments**. Do not use them to override the current architecture, issues, ADRs, or the mockup implementation notes.

Historical files:

- [`legacy/overview.md`](legacy/overview.md)
- [`legacy/features.md`](legacy/features.md)
- [`legacy/workflow.md`](legacy/workflow.md)
- [`legacy/enhancements.md`](legacy/enhancements.md)
- [`legacy/mockups.png`](legacy/mockups.png)

## Documentation boundary

New design artifacts should be added under `docs/design/`; do not recreate a top-level `design/` tree.
