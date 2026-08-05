# Eyespie 2026 Release Plan

## Release intent

Eyespie will target two bounded release stages:

| Stage | Target date | Audience | Purpose |
|---|---:|---|---|
| Closed alpha | September 30, 2026 | Invited TestFlight and Play Internal Testing users | Prove the complete travel-spy game loop on real devices and collect actionable failures |
| Public beta | October 31, 2026 | Broader external testers | Ship a supportable, accurately documented core experience with rollback capability |

The dates are targets, not permission to bypass release gates. A missed gate moves scope or date; it does not weaken validation.

## Core release scope

The first release is the smallest coherent travel-spy game:

1. A player installs and completes onboarding.
2. The player captures an outdoor subject and creates a challenge.
3. Eyespie produces a valid clue and a canonical image embedding.
4. The challenge is persisted and shared with another test account.
5. The second player captures a guess.
6. Eyespie compares the guess using the same embedding contract and reports match or non-match predictably.
7. Both players can recover from permission, network, model, and backend failures without losing navigation control.

## Explicit non-blockers for closed alpha

The following work must not block the September alpha unless it becomes a dependency of the core loop:

- the approximately 584 MB optional GenAI model;
- iOS GenAI image input;
- AR or spatial gameplay;
- teams, leaderboards, streaks, badges, and achievements;
- comprehensive offline synchronization;
- Bluebell extraction;
- broad framework modernization unrelated to release correctness.

## Capability boundaries

### MediaPipe iOS distribution

Eyespie consumes a project-specific MediaPipe iOS `0.10.26.1` distribution containing coherent Common, Vision, GenAIC, and GenAI frameworks.

The release boundary is not satisfied by published archives alone. The repository must prove:

- deterministic CocoaPods resolution from immutable podspecs and archives;
- one coherent MediaPipe and protobuf family;
- Kotlin/Native cinterop compilation;
- static KMP framework linkage;
- successful unsigned simulator application build;
- runtime construction of the Vision task used by Eyespie;
- bounded diagnostics when pod resolution or native linking fails.

The public-source iOS GenAI implementation is CPU-only. Image input is unsupported and must not be advertised or silently attempted.

### Image embeddings

Android and iOS must share an explicit embedding contract:

- model identity and version;
- vector dimension;
- float or quantized representation;
- normalization policy;
- byte order and serialization format;
- backend storage and RPC expectations;
- compatibility and migration rules.

A deterministic byte hash is acceptable only as a test double. It is not semantic image matching.

### Clue generation

Release code must not destructure arbitrary free-form model output. Clues require a versioned structured schema, validation, controlled failure behavior, and provenance sufficient to identify the provider, model, prompt, and schema version.

A deterministic or template-based clue path is acceptable as a release fallback.

## Critical path

```text
custom MediaPipe release
        ↓
deterministic CocoaPods install
        ↓
Kotlin/Native and static framework link
        ↓
real iOS app build
        ↓
Vision runtime construction
        ↓
cross-platform embedding contract
        ↓
validated camera conversion
        ↓
structured clue generation
        ↓
end-to-end two-player device test
        ↓
signed internal distribution
        ↓
closed alpha
```

## Closed-alpha exit criteria

- [ ] PR integrating the custom MediaPipe pods is green from a clean checkout.
- [ ] CocoaPods SBOM preparation resolves the same locked graph as the application build.
- [ ] Android and iOS construct and use MediaPipe Vision on physical devices.
- [ ] Android and iOS embeddings conform to the documented backend contract.
- [ ] Camera conversion and frame ownership are validated under sustained use.
- [ ] Malformed clue output cannot crash the flow.
- [ ] A complete two-player game succeeds repeatedly on Android and iOS.
- [ ] Signed builds install, launch, upgrade, and relaunch through TestFlight and Play Internal Testing.
- [ ] Permission denial, network loss, backend failure, and model/runtime failure degrade predictably.
- [ ] Privacy disclosures match actual image, embedding, location, network, and telemetry behavior.
- [ ] README, landing page, versioning, and release notes describe only verified capabilities.
- [ ] A rollback build and dependency rollback procedure exist.

## Public-beta exit criteria

- [ ] Closed-alpha crash and completion failures have been triaged and addressed.
- [ ] Matching thresholds are calibrated against representative non-sensitive fixtures.
- [ ] Store metadata, screenshots, privacy information, support path, and release notes are complete.
- [ ] Production backend migrations and configuration are reproducible.
- [ ] Observability reports actionable diagnostic codes without sensitive payloads or private paths.
- [ ] Release and rollback have been rehearsed against the exact candidate build.

## Release policy

- Do not claim unsupported iOS GenAI image input.
- Do not treat successful native linking as runtime feature proof.
- Do not ship raw fallback behavior that silently changes matching semantics by platform.
- Do not allow store deadlines to weaken integrity, lockfile, SBOM, permission, or privacy controls.
- Prefer removing an unfinished feature from the release over shipping an ambiguous or misleading implementation.
