# Wayfinder visual review checklist

Parent: #285. Runtime implementation: #286. Deterministic references: #287 / PR #290. Composition convergence: #291 / PR #292.

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

The hosted Android screen suite also captures one normal-font screenshot from the actually installed app after explicitly staging and offline-verifying the pinned runtime model, reinstalling the debug APK, clearing app data, and launching the production activity. This is review evidence, not a pixel-diff gate.

## First exact board review — 2026-09-01

PR #290's first Pixel-5 references were rendered and inspected as a contact sheet, then compared directly with all eight canonical board tiles (`01.webp`–`08.webp`).

### What matched the intended direction

Across the reviewed states, the app consistently had:

- a deliberate Micrantha palette rather than stale violet/default Material roles;
- recognizable field/case hierarchy;
- distinct primary and secondary actions;
- explicit local-mode/privacy language;
- thumbnail/target-style row affordances where data exists;
- explicit progress and success text rather than color-only state;
- a camera field that remains visually dominant;
- signed import provenance without unsupported secrecy or conflict actions.

The references were accepted as a deterministic **current-state baseline**, while #291 retained the remaining composition gaps.

### Material composition gaps identified by that review

| Reference/state | Review result | Remaining delta |
| --- | --- | --- |
| Onboarding / Local | **PARTIAL — CONVERGENCE REQUIRED** | Canonical composition is compact and screen-native with top Skip and bottom-anchored Next. Large dossier composition left excessive unused lower space. |
| Home / empty | **PARTIAL — CONVERGENCE REQUIRED** | Board uses a compact app header/Local Mode row and keeps create/import near the lower game-list action area. |
| Home / populated | **PARTIAL — CONVERGENCE REQUIRED** | Rows aligned directionally, but header/card density and action placement remained materially different. |
| Verified import preview | **PARTIAL — CONVERGENCE REQUIRED** | Provenance/metadata/actions were correct, but preview lacked the board's dedicated focal composition. |
| Game detail / creator | **PARTIAL — CONVERGENCE REQUIRED** | Builder/share dossiers consumed most of the first viewport instead of prioritizing case identity/progress/clues. |
| Play / searching | **PARTIAL — CONVERGENCE REQUIRED** | Camera dominance was correct; overlays were heavy and full-width CTA was less camera-native. |
| Play / clue found | **PARTIAL — CONVERGENCE REQUIRED** | Explicit success semantics were good, but confirmation lacked the board's focused treatment. |
| Play / case complete | **MATCHES DIRECTION, REVIEW WITH #291** | Terminal success hierarchy was clear and bounded. |
| Utility / profile & privacy | **PARTIAL — CONVERGENCE REQUIRED** | Truthful content was substantially denser and more card-heavy than the compact settings direction. |

## Composition convergence review — PR #292

PR #292 changed only presentation composition plus its visual/installed-build evidence. The nine updated Roborazzi `actual` images were generated at the same Pixel-5/API-35 contract, inspected together, and accepted before being recorded as the replacement reference set. The comparator threshold remains `0.002` (0.2%); no tolerance was loosened to approve the change.

| Reference/state | Review result | Resolution / intentional divergence |
| --- | --- | --- |
| Onboarding / Local | **MATCHES DIRECTION** | Compact screen-native composition now has top Skip, central illustration/content, progress, and bottom-anchored primary action. Micrantha mark/palette intentionally replace generated violet branding. |
| Home / empty | **MATCHES DIRECTION** | Compact header and Local Mode row now lead into the game section; create/import sit after the case content rather than above it. |
| Home / populated | **MATCHES DIRECTION** | Thumbnail-led compact rows and game-first hierarchy materially match the board direction while preserving created/shared role badges. |
| Verified import preview | **MATCHES DIRECTION WITH INTENTIONAL DIVERGENCE** | Preview is now a dedicated incoming-case surface with strong signed-state focus and dominant Add action. The preview model has no trusted cover-photo field, so it uses a target/place placeholder rather than inventing image authority. |
| Game detail / creator | **MATCHES DIRECTION** | Case identity and progress lead directly into compact clue rows; creator authoring/share tools move below clue content. |
| Play / searching | **MATCHES DIRECTION** | Camera remains dominant; case/clue information is one compact translucent overlay and capture uses a centered camera-like shutter control with explicit accessibility text. |
| Play / clue found | **MATCHES DIRECTION WITH INTENTIONAL DIVERGENCE** | Confirmation is now focused and centered with explicit semantic success and next action. The play state does not retain a captured-photo field, so the result does not fabricate or persist an image solely for mockup parity. |
| Play / case complete | **MATCHES DIRECTION** | Existing terminal success composition remains clear after overlay compaction. |
| Utility / profile & privacy | **MATCHES DIRECTION** | Separate dossier cards are consolidated into one compact grouped settings surface with dividers. Truthful local identity/privacy/camera copy remains somewhat more explicit than the exploratory board. |

### Security and architecture review

The convergence pass does **not** change:

- route-scoped MVI or domain behavior;
- `.eyespie` schema/signature/conflict authority;
- SQLDelight/local-authoritative state;
- platform signing identity;
- MediaPipe model, embedding, or match policy;
- camera permission/native ownership or busy-state lifecycle guards;
- creator-only expected-answer boundaries.

The signed import copy still describes integrity/provenance rather than secrecy, and no hosted-account, Gallery import, unsafe replace/keep-both conflict action, or warmer/colder AR promise was reintroduced.

## Installed-build acceptance — 2026-09-02

The first installed-build evidence attempt correctly exposed a fail-closed startup because the ordinary offline debug APK did not contain the explicitly provisioned image-embedder model. The evidence workflow was corrected to follow the documented runtime lifecycle rather than bypassing startup:

1. stage the generation-pinned Android image-embedder model;
2. verify the staged bytes offline against the pinned size and SHA-256 manifest;
3. run the enlarged-font feature-screen interaction suite;
4. reinstall the exact debug APK emitted by that build;
5. clear the `com.micrantha.eyespie.debug` app state and launch the production activity at normal font scale;
6. verify Eyespie is the active window and capture the installed application.

Android screen instrumentation run `33608426665` passed that complete chain. The captured Pixel-5/API-35 installed application renders the real first onboarding state, not `AppUnavailable`: Micrantha mark/palette, top Skip, centered `1 / 4` progress and illustration, `LOCAL MODE` / `Play locally` hierarchy, local-authority copy, and bottom-anchored `Next` are all visible with no material clipping or safe-area regression.

**Installed-build visual review: ACCEPTED.**

### Final acceptance boundary

#291 / #285 are complete when the final PR head retains:

1. verify-mode Roborazzi reproduction of the approved images;
2. green core Android CI, enlarged-font screen instrumentation, workflow security, and iOS simulator application build;
3. the accepted installed-app Pixel-5 onboarding evidence above.
