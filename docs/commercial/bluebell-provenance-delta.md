# Bluebell Provenance Delta Matrix

## Status

Bounded current-tree comparison supporting #106 and `docs/commercial/provenance-evidence.md`.

This is a provenance aid, not a semantic code review and not a legal conclusion. It intentionally uses Git tree/blob identity where possible so byte-identical inherited source can be distinguished from current Eyespie-local divergence without manually diffing unchanged files.

## Baseline

Compared current trees:

```text
Eyespie:
  ryjen/eyespie
  bluebell/src/...

Bluebell SDK baseline:
  hackelia-micrantha/bluebell-sdk
  library/src/...
```

The SDK source was imported in commit `fa47e6d2d64f67f00f9b9585b37eca2ed466b402` on August 18, 2025; that commit also added the Apache-2.0 `LICENSE`.

## Common-main top level

| Path under `com/micrantha/bluebell` | Eyespie SHA | SDK SHA | Classification |
| --- | --- | --- | --- |
| `App.kt` | `aeb4448f0a9e0bb984e653f1387fb1d3cf70afa1` | `aeb4448f0a9e0bb984e653f1387fb1d3cf70afa1` | **byte-identical baseline** |
| `Module.kt` | `5ddf90f91614b0b3a1192d438d4ff71e1aca60be` | `dddfc155f642a1647049e58cb088b8470e11f709` | **diverged** |
| `app/` tree | `1af7b0a96433015bc546a211f39feb7082adce24` | `3b37845475a2ff68815e2ee33be63f9fd35e36d7` | **diverged tree** |
| `arch/` tree | `a1642028e4e255862ce7f2d1eb767ebcfa5aceff` | `a1642028e4e255862ce7f2d1eb767ebcfa5aceff` | **byte-identical whole tree** |
| `data/` tree | `c73ce66f9df1d63d8148ea30141eb301dc001047` | `6b80bdc3c99fee409e9dad738825b6edc9241c98` | **diverged tree** |
| `domain/` tree | `768f6939bfa42a2467512963d6bed7197e3bb300` | `664a237bd69bb734aa17913f1d628c14e091d6db` | **diverged tree** |
| `ext/` tree | `0d897f3d7828569840d7f1a6ef0c363189598af4` | `07956b4d0045bb2b0faab5004a6136efc093be40` | **diverged tree** |
| `flux/` tree | `69fd7f594e0bdd9dcbc09dbd987e8e2dfc5c4863` | `96ed01ff31c14fa8f4dd92080d5089560ee2b7d8` | **diverged tree** |
| `history/` tree | `64b2e6eab92196bf42918ae575bc838cec55c402` | `64b2e6eab92196bf42918ae575bc838cec55c402` | **byte-identical whole tree** |
| `i18n/` | present | not present at SDK top level | **Eyespie-local/additional at this baseline level** |
| `observability/` | present | not present at SDK top level | **Eyespie-local/additional at this baseline level** |
| `platform/` tree | `f3c81c0fa209651d68abd0faa60ceef7f23e7405` | `7619cd545efd280f63906f286058b36fdae5766b` | **diverged tree** |
| `ui/` tree | `c92a709b54739e2abc43b745527449205780aa4f` | `ded1081f382d2bfbfac4c38594956dc49bdf86c8` | **diverged tree** |

A matching Git tree SHA means all names/modes/blob references under that directory tree are identical at the compared revisions. `arch/` and `history/` therefore need no further content diff for current-byte provenance.

## Representative identical files inside otherwise broader lineage

Additional exact blob matches observed between current Eyespie local Bluebell and the SDK include:

- `app/Errors.kt` — `d6925511cf930ef419d59f479ee60298da42b6d4`
- `app/Scaffolding.kt` — `69634f8c8e16a3ced3c5240a821ab01322912bec`
- `app/navi/NavAction.kt` — `0c74a2ed5f675b0f1a1eadcdea97640104d6b4db`
- `app/navi/Router.kt` — `0398e9d9b61f3cd3021e381e45b0e8fead71188d`
- `arch/Dispatcher.kt` — `e64d8d0ba528cd4625ca8d22061c2667bccebb23`
- `arch/Effect.kt` — `b3d11e11de4b7917222a89e01950e6b16e8c1830`
- `arch/Reducer.kt` — `a89d091f7ef0d28265aa737b7cb7dd86a37d5948`
- `arch/State.kt` — `b22e525f4e17e7469916a34f56c174350af88a6f`
- `arch/Store.kt` — `e2a36cc80595a0816e6f9d165c55f8dff5994c95`

This demonstrates that a differing parent tree does not imply every file in it changed; the local module is a mix of inherited and divergent source.

## Android top level

Current Android source layout differs materially at the first package level:

| Path | Eyespie | SDK | Classification |
| --- | --- | --- | --- |
| `platform/` | tree `5c2ed64d539d1aa7481f63eae37377d688a3410e` | tree `c51d414379d2f6b4723d249b10abafc94002510a` | **diverged tree** |
| `data/` | absent at compared Eyespie package level | tree `056eaf4b8fbae08830a575ab00317b1f45d04347` | **SDK-only at current compared level** |

Representative Android files previously observed as byte-identical include:

- `platform/DisplayMessage.kt` — `d610a37fc5667b1c74bf360c3a494376c2c0e182`
- `platform/Image.kt` — `e747141b376f617c75ad01684256aaaa2257d860`

Representative current divergences/additions include Android `NetworkMonitor.kt`, `Platform.kt`, `GenAI.kt`, and `Locale.kt`.

## Historical chronology

For representative `App.kt`:

1. `bluebell-sdk` initial repository commit: August 15, 2025.
2. `bluebell-sdk` source import `fa47e6d...`: August 18, 2025; Apache-2.0 `LICENSE` and SDK source added together.
3. `App.kt` at that import has blob `aeb4448...`.
4. Eyespie old `euphrasia/.../bluebell/App.kt` first appears in the observed path history by `fd4ae8b...`, authored October 19, 2025, with the same blob `aeb4448...`.
5. Eyespie `1762b724...` on January 9, 2026 explicitly documents moving Bluebell source from the old `euphrasia/src/...` location into the standalone `bluebell/src/...` module.

This is strong technical evidence that the local module's shared baseline descends from the Apache-2.0 SDK source already present before the observed Eyespie copy.

## Provenance classification for #106

For commercial/license inventory purposes, classify current local Bluebell source as:

```text
A. Exact inherited Apache-2.0 baseline
   - identical blob/tree SHA
   - no current content diff required

B. Derived/diverged source
   - corresponding SDK path exists but SHA differs
   - local modification history may matter if a license/notice decision requires it

C. Eyespie-local/additional source
   - no corresponding path at the compared SDK level
   - provenance is Eyespie history unless another imported source is identified

D. SDK-only source
   - exists in current SDK but not local Eyespie module
   - not part of the local source merely because it exists upstream
```

Do not infer that category B or C has a different license automatically. This matrix records lineage/content identity; the intended combined-work licensing/distribution decision remains #106/legal review.

## Sufficient next depth

A deeper file-by-file comparison should be demand-driven:

- required if changing/relicensing Bluebell itself;
- useful if a disputed/provenance-sensitive file is identified;
- unnecessary merely to ship the closed alpha;
- unnecessary for directories already proven byte-identical by tree SHA.

For paid-launch readiness, higher-value remaining work is the **exact release-bundle SBOM/NOTICE/model/asset review** and the then-current store/legal decision, rather than exhaustively diffing every benign framework edit now.
