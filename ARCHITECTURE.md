# Eyespie Architecture

## Decision

Eyespie is **backendless-first and local-authoritative**. Core game creation, playable game data, clue authority, image embeddings, matching, cryptographic player identity, and progress live on the device. A hosted backend is an optional future adapter rather than foundational game authority.

The current backendless/local-authoritative architectural epoch is **Wayfinder**. The previous cloud-authoritative implementation remains recoverable from `archive/pre-backendless-reboot-2026-08-15` at `50091a631d971c520e48884cfbd15cf15dd7251b`.

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
          +-----------------+------------------+
          |                                    |
          v                                    v
scoped document IO                    native share presenter
Android / iOS                         Android / iOS
          |                                    |
          +-----------------+------------------+
                            |
                            v
                 external file/deep-link ingress
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

Authoring uses a two-phase presentation boundary: live full-screen capture first, then a bounded captured-still review surface with the authoring form. Captured image data remains transient route/operation state rather than durable navigation or game authority.

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

Import treats bytes as hostile until bounds, schema, identity, model/policy compatibility, domain validity, and P-256 signature verification succeed. A previously unseen valid game is presented as a verified preview first; explicit confirmation revalidates against current local authority before persistence. Repeated import is deterministic/idempotent or returns an explicit non-mutating conflict.

Signatures provide integrity and creator-key continuity. They do **not** provide confidentiality, DRM, verified human identity, or strong anti-cheat against a player who controls the device.

See [`docs/architecture/eyespie-bundle-v1.md`](docs/architecture/eyespie-bundle-v1.md).

## Native file sharing and external ingress

Wayfinder treats **share**, **save**, and **open** as distinct platform operations while keeping one signed `.eyespie` authority:

```text
Share game
  -> signed bounded .eyespie bytes
  -> temporary app-private artifact
  -> Android system chooser / iOS UIActivityViewController

Save game file
  -> same signed .eyespie bytes
  -> scoped Android/iOS document save UI

Open received game
  -> platform-owned URI/URL
  -> bounded read
  -> existing bundle verification
  -> verified preview
  -> explicit Add game
```

Platform URLs, URIs, file handles, temporary artifacts, security-scoped resources, chooser state, and native presentation objects remain below platform boundaries. Native share presentation means the OS accepted/presented the chooser; it is not proof that a recipient selected, received, or persisted the file.

External `.eyespie` inputs are hostile regardless of sender, MIME type, extension, file provider, or transport. They enter the same bounded verification and explicit-confirmation path as picker imports.

The app-owned deep-link contract is deliberately narrower than file transport:

```text
eyespie://game/<GameId>
```

A deep link may navigate only to an already accepted local game. It cannot carry bundle bytes, embeddings, signatures, hidden authority, or trigger an implicit download/import. Unknown or malformed links fail without changing game authority.

The final claim of Android ↔ iOS sharing/opening interoperability remains gated by physical #92/#299 evidence rather than simulator/fixture success alone.

## Wayfinder presentation boundary

Wayfinder presentation is feature-owned and route-scoped. Voyager owns the application back stack; feature outputs remain semantic. Reducers stay synchronous/pure, interactors own asynchronous capabilities/effects, and platform camera/file/share objects do not become durable MVI state.

The canonical whole-app visual direction and intentional divergences are documented under [`docs/design/`](docs/design/). Deterministic Android screenshot/golden references provide regression evidence for representative states, but they complement rather than replace interaction/accessibility tests or physical-device review.

## Operational diagnostics

Closed-alpha support uses an Eyespie-owned bounded operational diagnostics facade rather than provider telemetry SDKs in domain/features.

The current contract provides:

- a closed operation/result/diagnostic-code vocabulary across startup, camera, embedding, matching, persistence, signed bundle operations, and native handoffs;
- bounded in-memory/process-lifetime diagnostic history with deterministic eviction and explicit drop/incomplete accounting;
- W3C-shaped trace/span correlation for nested operations without product/user identifiers;
- exact bounded app/source/database/bundle/runtime/model/embedding/match-policy provenance where available;
- explicit JSON export through scoped native file UI;
- fail-open observation semantics: diagnostic failure/drop cannot become gameplay authority or fail a successful operation.

The portable diagnostic graph has no normal representation for image/frame bytes, embeddings, clue/answer text, bundle payloads, private filesystem paths, keys/tokens, exact location, recipient/contact identity, arbitrary exception bodies, environment dumps, or unrestricted attributes.

There is no automatic diagnostics upload or remote collector in the closed-alpha path. Operational diagnostics are distinct from future product analytics. Physical #304/#125 evidence must still confirm that exported diagnostics are useful/private and that third-party MediaPipe/runtime network behavior is understood.

## Release evidence boundary

Automated CI establishes implementation and integration prerequisites:

- candidate-identity verification;
- common/Python/Android tests;
- Android app and instrumentation-test APK builds;
- deterministic Wayfinder visual-reference checks;
- project-specific MediaPipe CocoaPods resolution;
- Kotlin/Native simulator compilation;
- real unsigned Xcode simulator application build;
- workflow-security checks;
- protected exact-source signed candidate construction when explicitly dispatched.

The closed-alpha release still requires manual/physical evidence:

- #239 — representative Android runtime initialization/readiness;
- #295/#297 — physical confirmation of candidate-changing Android UX fixes;
- #91 / #190 — Android+iPhone embedding reports and cross-platform comparison;
- #125 — physical network/third-party telemetry observation against the exact candidate;
- #92 / #299 — complete two-device native share/open/import/deep-link/create/guess flow in both directions;
- #304 — physical diagnostic-export privacy/usefulness/offline verification;
- #93 — protected signed Play Internal/TestFlight distribution plus install/upgrade/relaunch/recovery proof;
- #18 — final security/privacy sign-off based on observed behavior;
- #94 — final documentation/store/privacy claim reconciliation.

Committed procedures are under [`docs/release/`](docs/release/). Issue #90 remains the canonical release dependency/candidate tracker.

## Related repositories

### `ryjen/mediapipe`

Owns the project-specific Apple MediaPipe distribution/provenance boundary used by Eyespie. The current Vision/ImageEmbedder alpha remains on the reviewed project-specific family; GenAI/LiteRT LM migration research is explicitly post-alpha compatibility work and must not displace physical qualification unless evidence identifies a concrete release defect.

### `hackelia-micrantha/bluebell`

The canonical public Apache-2.0 reusable KMP SDK/framework source. Eyespie no longer vendors/restores the previous broad Bluebell runtime graph. Reuse individual abstractions only when a concrete application-owned need justifies the dependency.

`bluebell-community` is not a required dependency in the Wayfinder alpha graph. Shared abstractions from Bluebell/community should be adopted only when their lifecycle/authority contract actually matches Eyespie; native share/deep-link/diagnostic behavior remains Eyespie-local unless such a reusable contract is proven.

## Optional future capabilities

Optional adapters may later provide capabilities that genuinely need remote or peer authority, such as:

- encrypted backup and cross-device sync;
- public game discovery;
- remote/host-authoritative multiplayer;
- identity recovery/linking;
- remote diagnostics ingestion and product analytics;
- semantic/GenAI runtime alternatives;
- AR/spatial gameplay.

Those capabilities must preserve provider-neutral boundaries and introduce their own authorization, privacy, retention, compatibility, and recovery analysis. They are not implied by the backendless alpha core.

## Delivery state

Completed Wayfinder implementation slices:

1. backend-free KMP core and deterministic matching;
2. SQLDelight local-authoritative persistence;
3. platform-backed cryptographic identity;
4. Android/iOS capture and MediaPipe embeddings;
5. manual clue authority and complete local create/play/match flow;
6. canonical signed `.eyespie` v1 import/export with verified preview before persistence;
7. scoped Android/iOS document read/write;
8. whole-app Wayfinder MVI/product surface and deterministic visual references;
9. native Android/iOS share presentation, external `.eyespie` ingress, and bounded local-game deep links;
10. privacy-safe bounded operational diagnostics and explicit export;
11. candidate identity, physical evidence collectors, protected internal-distribution tooling, and release runbooks.

Current work is **qualification, not feature expansion**. Physical/release evidence should complete before dependency/runtime experiments, Bluebell extraction, hosted transport, AR, Mission/commerce/analytics, or other post-alpha implementation displaces the release path.
