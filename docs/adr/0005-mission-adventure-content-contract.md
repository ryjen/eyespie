# ADR-0005: Mission / Adventure Content Contract

## Status

Accepted for post-alpha architecture.

Implementation remains explicitly outside the closed-alpha critical path in #90.

## Context

Eyespie currently has two useful runtime concepts:

- `Game` is a live social/session container with expiry, player/thing limits, players, and `Thing` listings.
- `Thing` is mutable runtime challenge/evidence with creator, captured image, location, embedding, guesses, and guessed state.

Those types fit the current free two-player game loop, but they are not suitable as the durable authored definition of Mission Packs, Private Events, organization-hosted experiences, or later creator-authored adventures. Reusing them would mix immutable content with player/session state and make update, retirement, offline, verification, and purchase history ambiguous.

The semantic game engine also requires model/version-aware verification. #91 owns the concrete embedding contract; mission content must select approved verification policy without embedding platform runtime objects or arbitrary authoritative thresholds.

`docs/product/monetization.md` establishes Mission / Adventure as the long-lived product/content unit. #103 tracks this decision, while ADR-0006/#104 owns catalog and entitlement semantics.

## Decision

Eyespie will introduce a **versioned, declarative Mission / Adventure content layer** above the existing runtime `Game` and `Thing` models.

The architecture separates:

```text
Mission identity
    |
    +--> immutable MissionArtifact version
    |        |
    |        +--> MissionDefinition payload
    |        +--> contentDigest over canonical payload bytes
    |
    +--> MissionPublication
             |
             +--> active version
             +--> published / paused / retired
             +--> availability and moderation state

MissionArtifact + normalized entitlement
             |
             v
      MissionSession / run
             |
             +--> existing Game where social/session semantics are needed
             +--> existing Thing/evidence where a task produces challenge evidence
```

A definition is authored content. An artifact is the validated immutable payload plus its integrity identity. Publication is operational state. A session/run is player state. Entitlement is access state. These states must not be collapsed into one object.

## Terminology

### Mission

The stable logical identity of an adventure across revisions. `MissionId` survives content updates and must not be a store SKU, localized title, or database-row version.

### MissionDefinition

The canonical immutable content payload for one mission version. It contains schema/content identity and authored mission data, but **does not contain its own content digest**.

### MissionArtifact

The immutable transport/cache/persistence envelope around one `MissionDefinition` plus its `contentDigest`.

The digest is calculated over the canonical serialized `MissionDefinition` payload only. It is never included in its own hash input.

### MissionPublication

Mutable operational state that decides whether a specific immutable artifact/version may be discovered or started. It is separate so a mission can be paused immediately without rewriting downloaded content or historical records.

### MissionSession / run

Player/group runtime progress pinned to one exact artifact identity. It never silently migrates to a newer content version.

### Game

The existing `Game` remains a live social/session concept. A MissionSession may create/reference a `Game` for multiplayer/private-event behavior, but `Game` is not authored Mission content.

### Thing

The existing `Thing` remains runtime challenge/evidence. A task may create/reference a `Thing`, but authored task definitions are not Things because Things contain player-specific mutable creator/image/location/embedding/guess state.

## Identity and versioning

The content payload and integrity envelope are explicitly separate:

```text
MissionArtifact
  contentDigest: Digest
  definition: MissionDefinition

MissionDefinition
  schemaVersion: Int
  missionId: MissionId
  contentVersion: Int
  publisher: PublisherRef
  metadata: MissionMetadata
  geography: GeographicPolicy
  playPolicy: PlayPolicy
  tasks: List<MissionTaskDefinition>
  rewards: RewardPolicyRef?
  access: AccessPolicyRef
  safety: AuthoredSafetyMetadata
```

A validated artifact identity is:

```text
missionId + contentVersion + contentDigest
```

### `schemaVersion`

Versions the serialization/contract shape.

- positive integer;
- unsupported required versions fail closed;
- additive fields may be ignored only where the schema explicitly allows forward-compatible extension;
- semantic reinterpretation requires a schema-version change.

### `missionId`

Stable logical identity across content revisions.

### `contentVersion`

Monotonic immutable version within one `missionId`.

Changing user-visible bytes or gameplay-relevant content creates a new version. That includes task targets, clues, geography, verification/scoring profile, completion/reward policy, and safety-relevant instructions.

### `contentDigest`

Initially SHA-256 over the canonical serialized bytes of `MissionDefinition` only:

```text
canonicalPayload = canonicalize(MissionDefinition)
contentDigest = SHA-256(canonicalPayload)
artifact = MissionArtifact(contentDigest, MissionDefinition)
```

The digest field is outside the canonical payload and therefore cannot create a self-referential hash.

The digest supports cache integrity, exact artifact identity, diagnostics, and history. It is not authentication, authorization, DRM, or a substitute for authenticated transport/signature policy.

## Canonical serialization

Use a deterministic, versioned serialization contract suitable for KMP and backend validation. Shared Kotlin should prefer `kotlinx.serialization` unless a deliberate alternative is justified.

Canonicalization must define the exact bytes being hashed. Ordinary JSON object/map iteration order is not sufficient.

Requirements:

- canonical payload contains `MissionDefinition` and excludes envelope fields such as `contentDigest`;
- the same logical payload produces the same canonical bytes on all supported targets;
- no platform-specific model object or localized in-memory representation participates in the digest;
- decode -> validate -> canonicalize -> digest is deterministic;
- digest mismatch is a typed integrity failure.

If canonical JSON is used, its property ordering, number/string escaping, null/default-field behavior, and collection ordering must be explicitly defined/tested rather than inherited accidentally from a serializer implementation.

## Task contract

Conceptually:

```text
MissionTaskDefinition
  taskId: MissionTaskId
  order: Int
  metadata: TaskMetadata
  clue: TaskClueContent
  target: TaskTargetPolicy
  verification: VerificationProfileRef
  scoring: ScoringProfileRef
  completion: CompletionPolicy
  safety: TaskSafetyMetadata?
```

Requirements:

- task IDs are unique within one definition;
- ordering is explicit rather than inferred from map/JSON iteration;
- active sessions remain pinned when later versions add/remove/reorder tasks;
- history records `missionId`, `contentVersion`, and `taskId`;
- task IDs may survive editorial copy updates when the gameplay objective remains semantically the same;
- materially different objectives receive a new task ID.

## Player-visible versus authority-only data

Authoring can include data that must not be disclosed to players, such as hidden answers, privileged reference embeddings, moderation evidence, or anti-cheat validation material.

```text
Authoring source
   |
   +--> player-visible Mission package
   |      title / story / clues / permitted geography / instructions
   |
   +--> authority-only package
          hidden answers / privileged verification data /
          moderation evidence / protected operational metadata
```

Serializability does not imply distributability. A player package must be separately validated not to contain prohibited authority-only fields.

If future offline behavior requires local access to privileged validation material, that requires an explicit security/product decision with #16/#91/#107. Default architecture keeps authoritative secrets off untrusted clients where practical.

## Verification and scoring boundary

A mission task selects an **approved, versioned profile**, not arbitrary runtime parameters:

```text
VerificationProfileRef
  profileId
  profileVersion
  compatibleEmbeddingSchema
```

#91 owns embedding representation/model compatibility and final matching policy.

Mission content must not:

- import/store MediaPipe platform objects;
- bundle raw runtime model objects in domain definitions;
- reinterpret embeddings from incompatible schemas;
- allow paid content to weaken verification fairness;
- allow arbitrary creator-authored match thresholds or executable scoring logic.

Unsupported or incompatible profiles fail closed before the affected task is accepted. Scoring follows the same allowlisted/versioned-reference pattern.

## Clue and answer boundary

#12/#13 own generated/manual clue schema, provenance, and provider routing.

Mission content preserves the same rules:

- player-facing clue text is intentionally disclosed;
- expected answers and privileged generation/verification data are not exposed accidentally;
- generated content retains application-owned provenance where required;
- model-authored provider/model identity is not trusted.

## Geography

Geography is a language-neutral content/policy concept, not a UI-map implementation.

The contract should support at least:

```text
NONE
MISSION_AREA
TASK_AREAS
ROUTE_OR_ORDERED_AREAS   // future-compatible
```

It must express whether exact target location is intentionally disclosed or whether only a broader permitted area is exposed. Exact location/sensitive-site policy remains subject to #18 and the post-alpha #107 extension.

## Publisher and safety metadata

Publisher identity is separate from player identity, billing identity, and authorization.

Future-compatible publisher categories include official, player/host, organization, and creator. A `PublisherRef` is context/identity; it does not grant publishing permission.

Immutable authored safety/accessibility metadata may include:

- environment/activity classification;
- accessibility notes;
- age/suitability reference;
- physical hazards/access constraints;
- private-property/trespass declaration;
- author-provided safety instructions;
- policy version used during authoring/review.

Mutable moderation state belongs in MissionPublication, not MissionDefinition.

## Publication lifecycle

Logical lifecycle:

```text
draft -> review -> published -> paused -> retired
```

### Draft

Authoring may change freely; not normally discoverable/startable.

### Review

Candidate content is frozen into a candidate artifact for validation/safety/quality review.

### Published

Publication points to one immutable `(missionId, contentVersion, contentDigest)` artifact. Published content is never edited in place.

### Paused

No new ordinary sessions start.

At minimum distinguish:

- `OPERATIONAL` — temporary availability/content problem;
- `SAFETY` — suspected unsafe/abusive content requiring fail-safe behavior.

A safety pause may stop active play at the next enforceable boundary; it is not defeated by a session having cached the content artifact.

### Retired

No new sessions start. Historical version/session/completion identities remain resolvable. Retirement must not destructively erase artifacts required for history/support/audit.

Calendar availability windows are separate from publication lifecycle.

## Entitlement / access boundary

MissionDefinition contains only an opaque `AccessPolicyRef`.

It must not contain:

- Apple/Google product identifiers;
- receipts or transaction tokens;
- billing SDK types;
- mutable `isPremium` flags;
- price/currency as gameplay authority.

ADR-0006/#104 owns catalog and entitlement semantics.

The mission layer asks:

```text
can subject X start artifact (M,V,D) under access policy A?
```

It does not call a store SDK. Free content uses an explicit free/public access policy.

## Runtime session model

A MissionSession pins exact artifact identity:

```text
MissionSession
  sessionId
  missionId
  contentVersion
  contentDigest
  subject/group reference
  startedAt
  status
  taskProgress[]
  gameRef?
```

A newer published version does not silently migrate an active session.

An official single-player Mission may need no `Game`. A Private Event may associate one `Game` with a MissionSession. Organization content may create many sessions against the same artifact.

Task runtime evidence maps deliberately:

```text
MissionTaskDefinition
    -> MissionTaskProgress
        -> evidence reference(s)
            -> Thing or dedicated evidence record
```

Use a dedicated evidence record when task proof is not semantically a Thing challenge.

## Persistence direction

The existing alpha Supabase/SQLDelight `Game` and `Thing` schemas remain unchanged by this ADR.

Additive post-alpha direction:

```text
Mission
  mission_id
  publisher_ref
  created_at

MissionVersion
  mission_id
  content_version
  schema_version
  content_digest
  canonical_definition_json   // MissionDefinition payload only
  created_at

MissionPublication
  mission_id
  active_content_version
  active_content_digest
  lifecycle_state
  pause_reason?
  available_from?
  available_until?
  moderation/policy metadata

MissionSession
  session_id
  mission_id
  content_version
  content_digest
  subject/group reference
  status
  started_at
```

`canonical_definition_json` stores/hash-verifies the payload; `content_digest` is a separate column/envelope value. The digest is not duplicated inside the payload.

Versioned JSON/JSONB plus indexed metadata is acceptable initially. Normalize task fields only when query/index/referential needs justify it.

## Offline/cache behavior

Coordinate with #16 and ADR-0006.

Cache by exact artifact identity:

```text
(missionId, contentVersion, contentDigest)
```

Before use:

1. decode MissionArtifact;
2. validate schema/IDs/order/policy references;
3. canonicalize `definition` payload;
4. recompute digest;
5. constant-time compare where appropriate to the envelope digest;
6. reject mismatch/corruption with a typed diagnostic.

Additional requirements:

- never replace active-session version silently;
- preserve required historical/current definition while retention policy permits;
- reconcile publication/access state on reconnect;
- do not contact App Store/Play at every task transition;
- bound storage/eviction;
- treat safety-critical publication freshness separately from ordinary content freshness;
- corrupted content fails closed without destroying valid local progress/history.

A future safety-sensitive public/commercial mission may require bounded publication freshness before a new offline start. That policy belongs with #16/#107 rather than being encoded as a store check.

## Updates and historical records

Any change to canonical definition bytes produces a new content version and digest.

Active sessions remain pinned unless a safety policy requires stopping or an explicit migration design is invoked. Silent mid-session migration is prohibited.

Historical completion/session records retain enough identity to remain meaningful:

- missionId;
- contentVersion;
- contentDigest/resolvable artifact identity;
- completed task IDs;
- relevant verification/scoring profile versions when required for audit/evaluation.

## Validation rules

Reject an artifact before publication/cache/use when required invariants fail, including:

- unsupported schema version;
- malformed mission ID;
- invalid content version;
- digest mismatch between envelope and canonical definition payload;
- duplicate task IDs;
- invalid/ambiguous task order;
- zero tasks where play policy requires tasks;
- invalid geography;
- unsupported/incompatible verification/scoring profile;
- malformed access/safety policy references;
- prohibited authority-only fields in a player package.

Validation returns typed failures/stable diagnostics rather than UI exceptions or database errors.

## Security and trust boundaries

- Mission artifacts from network/cache/creator tooling are untrusted until validated.
- Publisher identity is not authorization.
- Player packages must not leak hidden answers/privileged anti-cheat material.
- Verification/scoring profiles are allowlisted application/backend policy.
- Safety/moderation lifecycle is server-authoritative for public/commercial publication.
- Exact location is sensitive and minimized/disclosed according to mission policy.
- A digest detects content mismatch/corruption; it does not prevent a modified client from bypassing client-side checks and is not DRM.
- Protected/paid content authority therefore relies on backend authorization/content-delivery boundaries where needed, while the client digest protects correctness of official artifact handling.

## Consequences

### Positive

- one content primitive supports free official missions, Mission Packs, Private Events, B2B, and later creator content;
- existing alpha Game/Thing models remain intact;
- immutable artifacts make caches, support, history, and purchases auditable;
- publication can pause unsafe content without mutating artifact bytes;
- store billing stays outside content/domain identity;
- verification stays compatible with #91;
- digest calculation is non-self-referential and portable.

### Costs

- introduces MissionDefinition/MissionArtifact/Publication/Session concepts;
- requires explicit task->runtime evidence mapping;
- requires canonical serialization/digest tooling;
- requires cache/version reconciliation;
- public/commercial publishing adds moderation operations.

These costs are accepted because collapsing these concerns becomes materially more expensive once content can be purchased or externally published.

## Implementation slices

### Slice A — shared contract and validator (#108)

- opaque mission/task/version/profile value types;
- serializable MissionDefinition and MissionArtifact envelope;
- deterministic validation and typed failures;
- canonical payload serializer + digest verification;
- common tests for schema/version/IDs/order/profiles and digest invariants;
- no backend/store/UI integration.

### Slice B — immutable persistence/cache (#109)

- additive Mission/MissionVersion/Publication persistence;
- store canonical definition payload separately from digest;
- cache by mission/version/digest;
- validate artifacts before cache/use;
- coordinate #16.

### Slice C — MissionSession integration (#110)

- pinned session/run identity;
- map group/private-event sessions to Game where needed;
- map task evidence to Thing/dedicated evidence deliberately;
- preserve historical identity.

### Slice D — publishing/safety lifecycle (#111)

- draft/review/publish/pause/retire state;
- publisher authorization boundary;
- emergency safety pause with #107;
- no creator marketplace required.

## Acceptance tests for implementation

At minimum cover:

- representative free official mission and Private Event template;
- two versions of one MissionId;
- same payload canonicalizes to identical bytes/digest repeatedly;
- changing only envelope `contentDigest` does not change canonical payload bytes;
- digest mismatch rejection;
- unsupported schema rejection;
- duplicate task ID/order rejection;
- incompatible verification profile rejection;
- player package rejects authority-only fields;
- active session remains pinned when a newer version publishes;
- operational/safety pause semantics;
- retired content remains historically resolvable;
- offline artifact revalidates by payload+digest;
- existing alpha Game/Thing behavior remains unaffected until explicit integration slices land.

## Non-goals

- App Store/Google Play billing;
- SKU design/pricing;
- entitlement implementation (ADR-0006/#104);
- creator marketplace/payouts;
- full organization admin portal;
- public discovery/ranking;
- replacing current alpha Game/Thing;
- changing #91 embedding implementation;
- changing #12/#13 clue/provider behavior;
- expanding #90.

## Related work

- #16 — offline-first/cache behavior.
- #18 — release-critical baseline privacy/security.
- #90 — closed-alpha/public-beta; this ADR is non-blocking.
- #91 — embedding/verification contract.
- #103 — completed Mission content design.
- ADR-0006/#104 — product catalog/entitlement contract.
- #107 — monetization/geofenced-UGC/commercial-fraud threat model.
- #108–#111 — implementation slices.
- `docs/product/monetization.md`.
- `docs/architecture/semantic-game-engine.md`.
