# Wayfinder visual contract

Status: implementation guidance for #285.

## Sources of truth

Eyespie has two complementary design inputs:

1. `docs/design/eyespie-app-mockups/` defines the **Wayfinder product language**: field-desk hierarchy, dossier-like panels, strong primary actions, compact status treatments, camera overlays, clue progression, and the travel-spy tone.
2. `docs/design/brand/` defines the **Micrantha Lens brand**: canonical mark and palette.

Where a generated mockup conflicts with authority, security, platform, accessibility, or lifecycle contracts, the corrections in the mockup README remain normative.

The design goal is recognizable visual conformance, not brittle pixel identity.

## Palette reconciliation

The Micrantha palette is the only app-level palette:

| Role | Brand token | Value | Use |
| --- | --- | --- | --- |
| App field | `field` | `#D9E3DF` | full-screen background / field canvas |
| Primary ink | `ink` | `#314956` | primary actions and strong text |
| Deep ink | `pupil` | `#263947` | high-contrast text / inverse surfaces |
| Accent | `petal-inner` | `#6F8CA8` | eyebrows, progress, selected emphasis |
| Soft accent | `petal` | `#829FC0` | outlines and secondary emphasis |
| Warm surface | `throat` | `#EEE7CD` | secondary containers / briefing emphasis |
| Reward accent | `iris` | `#B59C69` | restrained tertiary emphasis |
| Paper surface | `white` | `#F5F5F0` | dossier cards and readable content surfaces |

Semantic success remains a separate muted green role. Verification, local-mode, and clue-found state must not be represented by brand accent alone.

There must not be a second violet/default Material palette in the app runtime.

## Geometry and density

- Base app content keeps platform safe drawing insets.
- Primary vertical rhythm: 18–20 dp between major sections; 10–12 dp inside grouped content.
- Dossier/panel shape: large rounded rectangle, approximately 24 dp radius through `MaterialTheme.shapes.large`.
- Panel treatment: paper-like surface, low/no elevation, subtle brand-outline border.
- Action height: at least 54 dp.
- Primary action: filled deep-ink button.
- Secondary action: outlined button using the brand outline role.
- Camera screens use translucent dossier panels over the live preview rather than opaque generic cards.

## Typography hierarchy

Use the existing accessible Material type scale, but apply a stable Eyespie hierarchy:

1. **Eyebrow** — uppercase, bold, tracked, accent color. Identifies mode or field context.
2. **Headline** — strong screen/case title.
3. **Body** — operational explanation or clue content.
4. **Status badge** — compact bounded state label; never rely on color alone.

Avoid turning every section heading into a generic Material headline. Eyebrow + content title is the canonical field-dossier pattern.

## Reusable primitives

`presentation/theme/WayfinderVisuals.kt` owns the current reusable treatment:

- `EyespiePanel`
- `EyespieEyebrow`
- `EyespieHeader`
- `EyespieSectionHeader`
- `EyespiePrimaryAction`
- `EyespieSecondaryAction`
- `EyespieTopBar`
- `EyespieStatusBadge`

These wrap Material rather than replacing it. Material remains responsible for interaction semantics, minimum target behavior, text scaling, and platform accessibility.

## Screen contract

### Onboarding

Treat each page as a field briefing: progress badge + Micrantha mark, illustration inside one paper dossier, eyebrow/title/body hierarchy, one dominant continue action, secondary back/skip actions.

### Field desk / Home

The home screen must read as a field desk rather than a generic settings/list screen:

- branded field heading + local agent identity;
- prominent Local Mode state;
- clear create/import actions;
- a `YOUR GAMES` dossier section;
- game rows with visual target tile, progress and role badge;
- utility/help as secondary navigation.

### Create and clue authoring

Camera remains the visual field. Authoring controls sit in translucent dossier panels. Capture/commit is the single dominant bottom action.

### Game detail

Case title and progress lead. Creator actions are grouped as case-building and signed-handoff panels. Clues are dossier rows with target thumbnail/avatar, bounded status, and one obvious play/review action.

### Play

Camera remains the visual field. Current case progress and clue appear as compact overlays. Match feedback has three deliberately different states:

- searching / mismatch — warm neutral guidance;
- clue found — semantic success;
- case complete — semantic success with terminal hierarchy.

### Profile / settings / help

Use field-kit cards with explicit eyebrows (`Agent identity`, `Data boundary`, `Field manual`, `Capture permission`) instead of a generic preference list. Do not fabricate permission toggles that are not backed by platform state.

## Accessibility and platform invariants

Visual conformance must preserve:

- safe drawing insets;
- dynamic type / enlarged text with scrolling rather than clipping;
- semantic text/action labels used by screen interaction tests;
- camera permission recovery behavior;
- non-color status communication;
- route-scoped MVI and semantic navigation;
- local-authoritative security and privacy boundaries.

## Shared-framework boundary

The field-dossier visual language is Eyespie product identity and should remain in `ryjen/eyespie` for now. Bluebell may later receive generic accessibility/layout primitives only after another consumer demonstrates the same abstraction; do not move Eyespie brand or travel-spy policy into Bluebell prematurely.

MediaPipe remains below the presentation boundary. No visual-conformance work changes model ownership, camera resource ownership, embedding semantics, or runtime provenance.

## Validation

Current interaction instrumentation must render screens under the canonical `EyespieTheme`; testing screens under bare `MaterialTheme` is insufficient because it bypasses the product palette and shapes.

Follow-up visual-regression work should add deterministic reference rendering for representative Home, Onboarding, Game Detail, and Play states. Golden comparison should focus on hierarchy, spacing, palette roles and component geometry, with tolerances appropriate for platform text rasterization.
