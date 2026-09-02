# Wayfinder visual review checklist

Parent: #285. Runtime implementation: #286. Deterministic references: #287 / PR #290. Remaining convergence: #291.

This is the bounded human-review companion to the automated interaction, brand-contract, and Roborazzi gates. It records what must be compared against the eight canonical board tiles before #285 closes.

## Review target

Use a fixed representative Android viewport equivalent to the hosted Pixel 5 / API 35 screen target and normal system font scale for primary visual review. Repeat the existing enlarged-font interaction pass separately; do not tune normal-density screenshots by disabling dynamic type.

For each state below, compare the rendered app against the corresponding area of `docs/design/eyespie-app-mockups/` and record either `MATCHES DIRECTION`, `PARTIAL — CONVERGENCE REQUIRED`, or an intentional divergence with rationale.

| Surface/state | Visual checks | Intentional contract differences |
| --- | --- | --- |
| Onboarding / Local | field background; briefing/illustration; progress; one dominant Next action | Micrantha palette/mark supersede generated violet branding |
| Onboarding / Create | illustration and field-note hierarchy | same |
| Onboarding / Share | signed-file handoff visual; copy hierarchy | `.eyespie` is inspectable, not secret |
| Onboarding / Join | join/open-file hierarchy | document/open-in flow, never Gallery import |
| Home / empty | identity; Local Mode; create/import; `YOUR GAMES` hierarchy; empty state | local authority/privacy wording follows current contract |
| Home / game rows | target tile; progress; created/shared badge; row affordance | none beyond current domain data |
| Import preview | verified-file hierarchy; signed status; Add/Cancel actions | no replace/keep-both conflict behavior |
| Create game | camera as field; authoring composition; dominant capture/create action | platform camera ownership/permission semantics retained |
| Game detail / creator | case title/progress; authoring/share; clue rows | share uses native platform handoff |
| Clue authoring | camera field; field-note composition; creator-only answer explanation | creator-only authority remains absent from playable bundle |
| Play / searching | camera field; compact case progress; prominent clue; capture action | no unsupported warmer/colder AR promise |
| Play / mismatch | warm bounded guidance distinct from error/success | deterministic current match policy retained |
| Play / found | semantic-green confirmation; next-clue hierarchy | color is accompanied by explicit text/icon |
| Play / complete | terminal success hierarchy | persisted local progress wording retained |
| Profile/settings/help | agent/privacy/manual/camera hierarchy | no fabricated permission toggles or hosted-account controls |

## Reject conditions

Reject the candidate as visually non-conformant if any representative state still presents as generic Material composition because of:

- default app/background palette instead of Micrantha field/paper roles;
- missing field-context eyebrow/headline hierarchy;
- generic elevated cards where a deliberate product treatment is expected;
- primary and secondary actions with indistinguishable hierarchy;
- stale pre-Micrantha violet or crossed-spyglass branding;
- camera UI obscuring the field with unnecessary opaque chrome;
- progress/result state that relies only on color;
- layout that clips or creates inaccessible actions under enlarged text;
- any visual change that alters `.eyespie`, permission, local-authority, or creator-only security semantics.

## Automated evidence boundary

#286 makes the hosted feature-screen interaction suite render under the canonical `EyespieTheme` and extends brand verification to the actual runtime palette/artwork. #287 / PR #290 adds checked-in Roborazzi references for nine representative states. Golden verification catches unexpected visual drift, but it does not automatically declare the current image equivalent to the exploratory board.

A representative installed-build visual review remains required before #285 closes.

## First exact board review — 2026-09-01

PR #290's first Pixel-5 references were rendered and inspected as a contact sheet, then compared directly with all eight canonical board tiles (`01.webp`–`08.webp`).

### What now matches the intended direction

Across the reviewed states, the app consistently has:

- a deliberate Micrantha palette rather than stale violet/default Material roles;
- recognizable field/case hierarchy;
- distinct primary and secondary actions;
- explicit local-mode/privacy language;
- thumbnail/target-style row affordances where data exists;
- explicit progress and success text rather than color-only state;
- a camera field that remains visually dominant;
- signed import provenance without unsupported secrecy or conflict actions.

The references are therefore accepted as a deterministic **current-state baseline**.

### Remaining material composition gaps

| Reference/state | Review result | Remaining delta |
| --- | --- | --- |
| Onboarding / Local | **PARTIAL — CONVERGENCE REQUIRED** | Canonical composition is compact and screen-native with top Skip and bottom-anchored Next. Current large dossier panel plus high actions leaves too much unused lower space. |
| Home / empty | **PARTIAL — CONVERGENCE REQUIRED** | Board uses a compact app header/Local Mode row and keeps create/import near the lower game-list action area. Current Field Desk heading/local card are oversized and create/import sit above the list. |
| Home / populated | **PARTIAL — CONVERGENCE REQUIRED** | Rows are recognizably aligned with the board, but overall header/card density and action placement remain materially different. |
| Verified import preview | **PARTIAL — CONVERGENCE REQUIRED** | Provenance/metadata/actions are correct, but the board gives preview its own stronger focal composition with target thumbnail; current preview remains embedded in Field Desk. |
| Game detail / creator | **PARTIAL — CONVERGENCE REQUIRED** | Board prioritizes case/target identity, metadata and compact clues. Current builder/share dossiers consume most of the first viewport and only one clue row is initially visible. |
| Play / searching | **PARTIAL — CONVERGENCE REQUIRED** | Camera dominance is correct; overlays are heavier than the board and the full-width CTA is less camera-native than the board's capture treatment. |
| Play / clue found | **PARTIAL — CONVERGENCE REQUIRED** | Explicit success semantics are good, but the board uses a more focused confirmation treatment with target imagery; current success panel is top-weighted and text-only. |
| Play / case complete | **MATCHES DIRECTION, REVIEW WITH #291** | Terminal success hierarchy is clear and bounded; re-review after play/found composition changes for consistency. |
| Utility / profile & privacy | **PARTIAL — CONVERGENCE REQUIRED** | Content truth is stronger than the generated board, but the current stacked dossier treatment is substantially denser than the compact settings list direction. |

### Decision

Do **not** close #285 on the strength of the first golden set.

- #287 is satisfied by deterministic checked-in references plus verify-mode CI.
- #291 owns the remaining screen-composition convergence exposed by this review.
- #285 closes only after #291 is accepted and one representative installed-build visual review finds no remaining material divergence.

The board remains directional, not a pixel-exact contract. #291 should move composition materially closer without reintroducing unsupported hosted accounts, Gallery import, unsafe conflict actions, secrecy claims, or non-semantic camera controls.
