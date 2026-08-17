# Eyespie Architecture

## Decision

Eyespie is **backendless-first and local-authoritative**. Core game creation, playable game data, clue authority, image embeddings, matching, cryptographic player identity, and progress live on the device. A hosted backend is an optional future adapter rather than foundational game authority.

The previous cloud-authoritative implementation remains recoverable from `archive/pre-backendless-reboot-2026-08-15` at `50091a631d971c520e48884cfbd15cf15dd7251b`.

## Current product architecture

```text
platform-backed P-256 identity
        |
        +-------------------+
        |                   |
        v                   v
SQLDelight local       authored bundle signing
Game/Thing/progress         |
        |                   v
        |            canonical .eyespie v1
        |                   |
        v                   v
bounded camera ---> MediaPipe ImageEmbedder
        |                   |
        +---------> validated 1024-float embedding
                            |
manual clue authority -----+
                            |
                            v
                       LocalGameLoop
                            |
                       MatchEngine
                            |
                    persisted progress
                            |
             scoped system document transfer
                   Android / iOS
```

Core alpha play does not require Supabase, hosted authentication, remote persistence, server-side matching, GraphQL/PostgREST/realtime, or an application-controlled network connection.

## Authority and trust boundaries

### Local identity

The default player identity is device-local and cryptographic:

1. generate/retain a P-256 signing key through platform security facilities;
2. expose only the canonical public key to common code;
3. derive stable `PlayerId` from that canonical public key;
4. sign locally authored portable bundles without exporting private key material.

`PlayerId` proves continuity of a local signing identity. It does not prove real-world human identity, attestation, or account ownership.

### Local persistence

SQLDelight is authoritative for local Game, Thing, clue-authority, and progress state. Repository APIs expose local semantics directly rather than network-first/cache-fallback behavior.

The SQLDelight embedding blob is a local storage representation. It is deliberately distinct from the portable `.eyespie` wire representation.

### Capture and embeddings

Platform code owns camera/native lifecycle:

- Android: CameraX;
- iOS: AVFoundation;
- common code receives bounded application-owned capture data rather than platform camera objects;
- MediaPipe ImageEmbedder produces the reviewed logical 1024-float embedding contract;
- malformed, wrong-dimension, or non-finite embeddings fail closed.

Physical-device parity, repeated-inference stability, and resource behavior remain release evidence under #91/#190.

### Clue authority

Manual clue authoring is sufficient for the closed alpha and requires no generative model or remote provider. Creator-only expected-answer authority is structurally excluded from playable/shared projections.

Optional semantic/GenAI providers are post-alpha/evidence-driven and may not become an implicit requirement for basic play while #90 remains open.

## Matching

Matching is local:

```text
camera capture
    -> MediaPipe ImageEmbedder
    -> validated embedding
    -> cosine similarity
    -> explicit Thing match policy/threshold
    -> MatchEngine
    -> match / non-match
```

No database vector search or hosted match RPC is required for target-specific gameplay.

## Portable `.eyespie` v1

The implemented portable format is one bounded canonical file, not an archive. It includes only the data required for offline play, including creator public identity, bounded game/Thing metadata, playable clue data, target embedding, explicit model/match compatibility identity, and signature metadata.

It deliberately excludes creator-only expected answers, private keys, raw target images, exact location, private filesystem paths, account/backend tokens, raw model prompts/output, and executable/archive content.

Import treats bytes as hostile until bounds, schema, identity, model/policy compatibility, domain validity, and P-256 signature verification succeed. Persistence occurs only after validation; repeated import is deterministic/idempotent or returns an explicit conflict.

Signatures provide integrity and creator-key continuity. They do **not** provide confidentiality, DRM, verified human identity, or strong anti-cheat against a player who controls the device.

See [`docs/architecture/eyespie-bundle-v1.md`](docs/architecture/eyespie-bundle-v1.md).

## Platform sharing

Android and iOS expose `.eyespie` import/export through scoped system document APIs. Platform URLs, URIs, file handles, security-scoped resources, and picker state remain in platform code and never become common game authority. Reads are bounded before common parsing and no broad storage/media permission is required.

The final release claim of Android ↔ iOS interoperability is gated by physical #92 evidence rather than simulator/fixture success alone.

## Release evidence boundary

Automated CI establishes implementation and integration prerequisites:

- candidate-identity verification;
- common/Python/Android tests;
- Android app and instrumentation-test APK builds;
- project-specific MediaPipe CocoaPods resolution;
- Kotlin/Native simulator compilation;
- real unsigned Xcode simulator application build;
- workflow-security checks.

The closed-alpha release still requires manual/physical evidence:

- #91 / #190 — Android+iPhone embedding reports and cross-platform comparison;
- #125 — physical network/telemetry observation against the exact candidate;
- #92 — complete two-device create/share/import/guess flow in both directions;
- #93 — protected signed Play Internal/TestFlight distribution plus install/upgrade/relaunch/recovery proof;
- #18 — final security/privacy sign-off based on observed behavior;
- #94 — final documentation/store/privacy claim reconciliation.

Committed procedures are under [`docs/release/`](docs/release/).

## Related repositories

### `ryjen/mediapipe`

Owns the project-specific Apple MediaPipe distribution/provenance boundary used by Eyespie. Its remaining GenAI, binary-size, performance, and upstream-convergence work must not implicitly block the current Vision/ImageEmbedder alpha unless physical evidence identifies a concrete defect.

### `hackelia-micrantha/bluebell`

The canonical public Apache-2.0 reusable KMP SDK/framework source. Eyespie no longer vendors/restores the previous broad Bluebell runtime graph. Reuse individual abstractions only when a concrete application-owned need justifies the dependency.

There is currently no required `bluebell-community` repository in the Eyespie alpha dependency graph.

## Optional future capabilities

Optional adapters may later provide capabilities that genuinely need remote or peer authority, such as:

- encrypted backup and cross-device sync;
- public game discovery;
- remote/host-authoritative multiplayer;
- identity recovery/linking;
- explicitly governed diagnostics/analytics;
- semantic/GenAI runtime alternatives;
- AR/spatial gameplay.

Those capabilities must preserve provider-neutral boundaries and introduce their own authorization, privacy, retention, compatibility, and recovery analysis. They are not implied by the backendless alpha core.

## Delivery state

Completed alpha implementation slices:

1. backend-free KMP core and deterministic matching;
2. SQLDelight local-authoritative persistence;
3. platform-backed cryptographic identity;
4. Android/iOS capture and MediaPipe embeddings;
5. manual clue authority and complete local create/play/match flow;
6. canonical signed `.eyespie` v1 import/export;
7. scoped Android/iOS document transfer;
8. candidate identity, physical evidence collectors, protected internal-distribution tooling, and release runbooks.

Current work is **qualification, not feature expansion**. Physical/release evidence should complete before dependency/runtime experiments, Bluebell extraction, hosted transport, AR, Mission/commerce/analytics, or other post-alpha implementation displaces the release path.
