# Commercial Provenance Evidence

## Status

Supporting evidence for #106 and `docs/commercial/commercial-distribution-inventory.md`.

This document records repository-history and source-lineage observations. It is not a legal chain-of-title opinion and does not establish store-distribution compatibility.

## Eyespie contributor/history evidence

### Contributor API

The current GitHub contributor view for `ryjen/eyespie` reports one human account (`ryjen`) plus automation accounts.

This is useful triage evidence but is not sufficient by itself to establish ownership of copied source, generated assets, vendor code, imported media, or contributions hidden by squash/history transformations.

### Default-branch commit-history scan

The current default-branch history was scanned across five non-empty pages of 100 commits; page six was empty.

Searches for `Co-authored-by` found no human co-author trailer. The co-author trailers observed were automation identities (Copilot/Dependabot).

This strengthens the evidence that the current default-branch commit history is authored by the repository owner, but it remains bounded evidence:

- it does not inspect unreachable history or every historical branch/fork;
- it does not prove authorship of copied/adapted snippets;
- it does not prove provenance of generated/vendor assets;
- it is not a substitute for legal review if relicensing/additional permissions are material.

## Bluebell source-lineage evidence

Eyespie contains a local KMP `:bluebell` module under `bluebell/src/...`.

The current `hackelia-micrantha/bluebell-sdk` repository contains a `library/src/...` SDK tree under Apache-2.0. The current public `hackelia-micrantha/bluebell` main tree is primarily the public/documentation presence rather than the full SDK source tree.

### Exact blob matches

A number of Eyespie local Bluebell files have the exact same Git blob SHA as files in `bluebell-sdk/library/src`.

Representative examples observed:

| File | Eyespie/local blob | `bluebell-sdk` blob | Observation |
| --- | --- | --- | --- |
| `commonMain/.../App.kt` | `aeb4448f0a9e0bb984e653f1387fb1d3cf70afa1` | same | byte-identical |
| `commonMain/.../app/Errors.kt` | `d6925511cf930ef419d59f479ee60298da42b6d4` | same | byte-identical |
| `commonMain/.../app/Scaffolding.kt` | `69634f8c8e16a3ced3c5240a821ab01322912bec` | same | byte-identical |
| `commonMain/.../app/navi/NavAction.kt` | `0c74a2ed5f675b0f1a1eadcdea97640104d6b4db` | same | byte-identical |
| `commonMain/.../app/navi/Router.kt` | `0398e9d9b61f3cd3021e381e45b0e8fead71188d` | same | byte-identical |
| `commonMain/.../arch/Dispatcher.kt` | `e64d8d0ba528cd4625ca8d22061c2667bccebb23` | same | byte-identical |
| `commonMain/.../arch/Effect.kt` | `b3d11e11de4b7917222a89e01950e6b16e8c1830` | same | byte-identical |
| `commonMain/.../arch/Reducer.kt` | `a89d091f7ef0d28265aa737b7cb7dd86a37d5948` | same | byte-identical |
| `commonMain/.../arch/State.kt` | `b22e525f4e17e7469916a34f56c174350af88a6f` | same | byte-identical |
| `commonMain/.../arch/Store.kt` | `e2a36cc80595a0816e6f9d165c55f8dff5994c95` | same | byte-identical |
| `androidMain/.../platform/DisplayMessage.kt` | `d610a37fc5667b1c74bf360c3a494376c2c0e182` | same | byte-identical |
| `androidMain/.../platform/Image.kt` | `e747141b376f617c75ad01684256aaaa2257d860` | same | byte-identical |

The local module is not simply a frozen copy. Representative current divergences include `Module.kt`, Android `NetworkMonitor.kt`, Android `Platform.kt`, and project-specific additions such as Android `GenAI.kt`/`Locale.kt`.

### Chronology for representative `App.kt`

The strongest representative chronology observed is:

1. `hackelia-micrantha/bluebell-sdk` was created August 15, 2025.
2. On August 18, 2025, commit `fa47e6d2d64f67f00f9b9585b37eca2ed466b402` (`feat!: initial import`) imported the SDK source tree and added the Apache-2.0 `LICENSE` in that same commit.
3. That import contains `library/src/commonMain/kotlin/com/micrantha/bluebell/App.kt` with blob `aeb4448f0a9e0bb984e653f1387fb1d3cf70afa1`.
4. Eyespie history shows the identical `App.kt` blob in the older `euphrasia/src/.../bluebell/App.kt` tree by commit `fd4ae8b2c87b6ecf90bffe030c7a8efa8ee00826`, authored October 19, 2025.
5. Eyespie commit `1762b724b2dcd0863e6c43ea00054ae91ffcf1c1` on January 9, 2026 explicitly states that Bluebell sources were moved from `euphrasia/src/...` into the standalone `bluebell/src/...` module.

For this representative file, the observed chronology plus the identical blob strongly supports `bluebell-sdk` as the earlier Apache-2.0 source baseline used by Eyespie.

This does **not** prove that every local Bluebell file came from the exact same SDK commit. Some local files have diverged or were added after the baseline. The commercial review should therefore treat the local module as:

```text
Apache-2.0 Bluebell SDK baseline
        +
Eyespie-local modifications/additions
```

and inventory the local deltas rather than treating the module as an opaque third-party binary.

## Remaining Bluebell provenance work

The provenance question is narrowed substantially. Remaining work is:

- identify which local files are exact baseline copies versus locally modified/additional files;
- where needed, identify the first local modification commit for divergent files;
- preserve Apache-2.0 attribution/license obligations for baseline-derived source;
- verify whether any private-only SDK code not covered by the observed Apache-2.0 history enters the distributed app;
- keep the legal conclusion about combined GPLv3 distribution separate from the factual source-lineage inventory.

A full file-by-file provenance matrix is useful before a licensing change, but it is not required for the closed alpha.

## MediaPipe and model provenance remain separate

This Bluebell evidence does not resolve MediaPipe or model licensing.

- `ryjen/mediapipe` has its own Apache-2.0 source/distribution boundary and transitive NOTICE review.
- the optional Gemma `.task` model remains a separate model-weight license/provenance item;
- exact release SBOMs and produced binaries remain authoritative for what is actually shipped.

## Implication for #106

The repository evidence now supports a more precise statement:

- no human co-authorship was found in the current Eyespie default-branch commit trailers;
- the local Bluebell module has strong, timestamped, byte-identical lineage to an Apache-2.0 `bluebell-sdk` source import predating the observed Eyespie copy;
- final relicensing authority still depends on resolving non-Git-history provenance (copied/generated/vendor/assets) and any material legal interpretation;
- #106 therefore remains open as a commercial-launch gate, while #90 remains unaffected.
