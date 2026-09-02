# Wayfinder visual review checklist

Parent: #285. Implementation: #286.

This is the bounded human-review companion to the automated interaction and brand-contract gates. It records what must be compared against the eight canonical board tiles before #285 closes.

## Review target

Use a fixed representative Android viewport equivalent to the hosted Pixel 5 / API 35 screen target and normal system font scale for primary visual review. Repeat the existing enlarged-font interaction pass separately; do not tune normal-density screenshots by disabling dynamic type.

For each state below, compare the rendered app against the corresponding area of `docs/design/eyespie-app-mockups/` and record either `MATCHES DIRECTION` or an intentional divergence with rationale.

| Surface/state | Visual checks | Intentional contract differences |
| --- | --- | --- |
| Onboarding / Local | field background; briefing dossier; illustration; progress badge; one dominant Next action | Micrantha palette/mark supersede generated violet branding |
| Onboarding / Create | illustration and field-note hierarchy | same |
| Onboarding / Share | signed-file handoff visual; copy hierarchy | `.eyespie` is inspectable, not secret |
| Onboarding / Join | join/open-file hierarchy | document/open-in flow, never Gallery import |
| Home / empty | Field desk identity; Local Mode; create/import; `YOUR GAMES` hierarchy; paper empty-state panel | local authority/privacy wording follows current contract |
| Home / game rows | target tile; progress; created/shared badge; row affordance | none beyond current domain data |
| Import preview | verified-file hierarchy; signed status; Add/Cancel actions | no replace/keep-both conflict behavior |
| Create game | camera as field; translucent dossier; dominant capture/create action | platform camera ownership/permission semantics retained |
| Game detail / creator | case title/progress; builder and signed-handoff panels; clue rows | share uses native platform handoff |
| Clue authoring | camera field; field-note dossier; creator-only answer explanation | creator-only authority remains absent from playable bundle |
| Play / searching | camera field; compact case progress; prominent clue; capture action | no unsupported warmer/colder AR promise |
| Play / mismatch | warm bounded guidance distinct from error/success | deterministic current match policy retained |
| Play / found | semantic-green confirmation; next-clue hierarchy | color is accompanied by explicit text/icon |
| Play / complete | terminal success hierarchy | persisted local progress wording retained |
| Profile/settings/help | field-kit dossier sections with agent/privacy/manual/camera hierarchy | no fabricated permission toggles or hosted-account controls |

## Reject conditions

Reject the candidate as visually non-conformant if any representative state still presents as generic Material composition because of:

- default app/background palette instead of Micrantha field/paper roles;
- missing field-context eyebrow/headline hierarchy;
- generic elevated cards where dossier panels are expected;
- primary and secondary actions with indistinguishable hierarchy;
- stale pre-Micrantha violet or crossed-spyglass branding;
- camera UI obscuring the field with unnecessary opaque chrome;
- progress/result state that relies only on color;
- layout that clips or creates inaccessible actions under enlarged text;
- any visual change that alters `.eyespie`, permission, local-authority, or creator-only security semantics.

## Automated evidence boundary

#286 makes the hosted feature-screen interaction suite render under the canonical `EyespieTheme` and extends brand verification to the actual runtime palette/artwork. That catches semantic interaction and brand-token drift, but it is not a screenshot comparison.

A focused follow-up should capture deterministic representative screen states and compare them against checked-in reference images with bounded tolerance. #285 remains open until that reference coverage and one installed-build visual review are accepted.
