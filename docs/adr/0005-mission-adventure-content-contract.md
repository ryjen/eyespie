# ADR-0005: Mission / Adventure Content Contract

## Status

Accepted for post-alpha architecture.

Implementation remains explicitly outside the closed-alpha critical path in #90.

## Context

Eyespie currently has two useful runtime concepts:

- `Game` is a live social/session container with an expiry, player/thing limits, players, and `Thing` listings.
- `Thing` is mutable runtime challenge/evidence with a creator, captured image, location, embedding, guesses, and guessed state.

Those types are appropriate for the current free two-player game loop, but they are not suitable as the durable definition of curated Mission Packs, Private Events, organization-hosted experiences, or later creator-authored adventures.

A commercial or curated mission needs stable identity, explicit content versions, lifecycle controls, verification compatibility, safety metadata, offline caching semantics, and a clean entitlement boundary. Reusing `Game` or `Thing` as that definition would mix authored content with mutable session/player state and make updates, retirement, offline use, and purchase history ambiguous.

The existing semantic game engine also requires model/version-aware verification rather than treating embeddings from different schemas as interchangeable. #91 owns the concrete image-embedding contract; mission content must reference approved verification policy without embedding runtime implementation details or arbitrary thresholds.

The monetization strategy in `docs/product/monetization.md` establishes Mission / Adventure as the long-lived product unit. #103 tracks this architecture decision. #104 will define catalog and entitlement semantics separately.

## Decision

Eyespie will introduce a **versioned, declarative Mission / Adventure content contract** above the existing runtime `Game` and `Thing` models.

The architecture separates five concepts:

```text
Mission identity
    |
    +--> immutable MissionDefinition version
    |        |
    |        +--> task definitions
    |        +--> presentation/content metadata
    |        +--> verification/scoring profile references
    |        +--> authored safety/accessibility metadata
    |
    +--> MissionPublication
             |
             +--> active version
             +--> published / paused / retired state
             +--> availability and moderation state

MissionDefinition + normalized entitlement
             |
             v
      MissionSession / run
             |
             +--> existing Game where a social host/session is needed
             +--> existing Thing/evidence objects where a task produces challenge evidence
```

A mission definition is content. A publication is operational state. A session/run is player state. An entitlement is access state. These states must not be collapsed into one object.

## Terminology

### Mission

The stable logical identity of an adventure across revisions.

Examples:

- `stanley-park-nature-hunt`
- `vancouver-spy-mission`
- a private event template
- an organization-authored city experience

A `MissionId` must remain stable while the content evolves.

### Mission definition

One immutable content version of a mission.

A published definition is never edited in place. Corrections or content changes produce a new content version.

### Mission publication

Mutable operational state deciding whether a specific mission/version may be discovered or started.

Publication state is deliberately separate from the immutable definition so a mission can be paused immediately without rewriting downloaded content or historical completion records.

### Mission session / run

A player's or group's runtime progress against one pinned mission version.

A run must not silently migrate to a newer content version after it starts.

### Game

The current `Game` entity remains a live social/session concept. A future mission run may create or reference a `Game` when multiplayer/team/session semantics are needed, but `Game` is not the authored Mission definition.

### Thing

The current `Thing` entity remains runtime challenge/evidence state. A mission task may eventually create, reference, or constrain a `Thing`, but `Thing` is not the authored task definition because it contains player-specific mutable state such as creator, guesses, image, embedding, and guessed status.

## Identity and versioning

Every immutable mission definition must carry at least:

```text
MissionDefinition
  schemaVersion
  missionId
  contentVersion
  contentDigest
  publisherRef
  metadata
  playPolicy
  tasks[]
  rewardPolicyRef?
  accessPolicyRef?
  authoredSafetyMetadata
```

### `schemaVersion`

Version of the serialization/contract shape understood by the application.

- positive integer;
- unknown newer required schema versions fail closed;
- additive fields may be ignored only where the schema explicitly permits forward-compatible extension;
- semantic changes require a new schema version rather than reinterpretation of an existing field.

### `missionId`

Stable logical identity across content revisions.

It must not be a store SKU, database row version, or localized title.

### `contentVersion`

Monotonic immutable version within one `missionId`.

Changing any gameplay-relevant content creates a new version, including target/task changes, clue changes, verification-profile changes, route/location changes, scoring policy changes, or safety-relevant instructions.

### `contentDigest`

Digest of the canonical immutable definition payload, initially SHA-256 unless an implementation ADR chooses another reviewed algorithm.

The digest supports cache integrity, diagnostics, and deterministic identity of the exact artifact used by a session. It is not a substitute for transport authentication or authorization.

## Proposed language-neutral contract

Exact Kotlin names may follow project conventions, but the serialized/domain boundary should be equivalent to:

```text
MissionDefinition
  schemaVersion: Int
  missionId: MissionId
  contentVersion: Int
  contentDigest: Digest
  publisher: PublisherRef
  metadata: MissionMetadata
  geography: GeographicPolicy
  playPolicy: PlayPolicy
  tasks: List<MissionTaskDefinition>
  rewards: RewardPolicyRef?
  access: AccessPolicyRef?
  safety: AuthoredSafetyMetadata

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

IDs are opaque values. UI strings, localized titles, store SKUs, model objects, receipts, platform permission types, and raw billing/provider payloads must not become identity fields.

## Task identity and ordering

Each task has a stable `taskId` within the mission identity.

Requirements:

- task IDs are unique within one mission definition;
- order is explicit rather than inferred from JSON/map iteration;
- a content revision may add/remove/reorder tasks, but an active run remains pinned to its original definition;
- completion history records `missionId`, `contentVersion`, and `taskId` so it remains interpretable after later revisions.

Task IDs should survive editorial copy changes where the underlying gameplay objective remains semantically the same. A materially different target/objective should receive a new task ID.

## Public content versus authority-only data

Mission authoring may contain information that must not be disclosed to a guesser/player, such as hidden answers, reference embeddings, moderation notes, or privileged validation material.

The contract therefore distinguishes **player-visible mission content** from **authority-only verification/content data**.

```text
Authoring source
   |
   +--> Player package
   |      title / story / clues / permitted geography / task instructions
   |
   +--> Authority package
          hidden answers / privileged reference data / moderation evidence /
          verification material not safe to disclose to an untrusted client
```

Do not assume that placing a field inside a serialized MissionDefinition makes it safe to distribute to the mobile client.

If a future offline mode requires local access to otherwise privileged verification material, that must be an explicit security/product decision with #16/#91/#107. The default architecture keeps authoritative secrets and anti-cheat material off untrusted clients where practical.

## Verification policy boundary

A mission task selects an **approved verification profile**, not arbitrary model parameters or numeric thresholds.

Conceptually:

```text
VerificationProfileRef
  profileId
  profileVersion
  compatibleEmbeddingSchema
```

#91 owns the canonical embedding representation/model identity and matching behavior. The mission layer may require compatibility with that contract, but it must not:

- store platform-specific MediaPipe objects;
- embed raw runtime model files;
- silently reinterpret embeddings from a different model/schema;
- let paid content weaken match thresholds as a purchase advantage;
- make arbitrary creator-authored thresholds authoritative.

Unsupported or incompatible verification profiles fail closed before a mission starts or before a task requiring that profile is accepted.

A similar reference pattern should be used for scoring policy so content selects from approved, versioned policies rather than embedding unrestricted executable/game-authority logic.

## Clue and answer boundary

#12/#13 own structured generated/manual clue provenance and provider routing.

Mission task content may reference curated or generated clue material, but must preserve the same disclosure rule:

- player-facing clue text is intentionally exposed;
- expected answers and generation/verification data are not accidentally exposed through player DTO/UI mappings;
- generated content retains trustworthy application-owned provenance where required;
- mission content does not trust model-authored provider/model identity.

## Geography

Geography is part of authored content but must remain a policy/domain concept rather than a UI map implementation.

The first contract should support:

```text
GeographicPolicy
  NONE
  MISSION_AREA
  TASK_AREAS
  ROUTE_OR_ORDERED_AREAS   (future-compatible)
```

A geographic area should use a language-neutral geometry/geofence representation or an opaque reference to one. The contract must be able to express:

- no geographic restriction;
- a bounded mission area;
- per-task bounded areas;
- whether exact target location is intentionally disclosed or only a broader permitted area is disclosed.

Exact location and sensitive-site behavior remains subject to #18 for the free baseline and #107 for public/commercial publishing.

## Publisher identity

Publisher identity is distinct from player identity, billing account identity, and store product identity.

The architecture must be able to represent at least:

```text
PublisherRef
  OFFICIAL
  PLAYER_OR_HOST
  ORGANIZATION
  CREATOR        (future)
```

Exact storage/authorization semantics may be introduced incrementally. The contract only requires a stable opaque publisher reference and publisher type where policy needs it.

A publisher reference does not itself grant publishing permission; authorization is a backend/application policy concern.

## Safety, accessibility, and age metadata

An immutable mission definition carries **authored metadata**, for example:

- intended environment/activity type;
- accessibility notes;
- age/suitability classification reference;
- declared physical hazards or access constraints;
- private-property/trespass declaration;
- author-supplied safety instructions;
- policy version under which the content was authored/reviewed.

Mutable moderation decisions do not belong inside the definition. They belong in `MissionPublication` / moderation state so an unsafe version can be paused without changing its content digest.

#107 owns the post-alpha commercial/geofenced-UGC threat-model extension.

## Publication lifecycle

The logical lifecycle is:

```text
draft -> review -> published -> paused -> retired
```

These states describe publication/operational availability, not immutable content mutation.

### Draft

- authoring in progress;
- not discoverable or startable by ordinary players;
- may change freely until a candidate immutable version is created.

### Review

- candidate content frozen for review;
- safety/quality/publisher validation may occur;
- not ordinary public availability.

### Published

- references one immutable `missionId + contentVersion + digest`;
- discoverable/startable according to audience/access/availability policy;
- cannot be edited in place.

### Paused

No new ordinary sessions start.

Pause records must distinguish at least:

- `OPERATIONAL` — temporary content/availability problem;
- `SAFETY` — suspected unsafe/abusive content requiring fail-safe behavior.

For a safety pause, clients/backend should stop affected active play at the next enforceable policy boundary rather than allowing an unsafe mission to continue solely because a session already started.

### Retired

- no new sessions start;
- historical completion/purchase/session records remain interpretable;
- cached content may be retained only according to offline/storage/license policy;
- retirement does not delete historical identities.

A retired mission may have a replacement mission/version, but the replacement is explicit rather than silently aliasing historical records.

## Availability window

Calendar availability is separate from publication lifecycle.

A published mission may additionally have a bounded availability window. Being outside the window prevents new starts but does not change the definition or erase historical records.

Time-window behavior for an already-started session must be explicit in the mission play policy rather than inferred ad hoc by UI code.

## Entitlement / access boundary

Mission content may contain only an opaque access requirement/reference such as `AccessPolicyRef`.

It must not contain:

- Apple product identifiers;
- Google Play product identifiers;
- transaction/receipt tokens;
- billing SDK types;
- mutable `isPremium` flags;
- price/currency as gameplay authority.

#104 owns product catalog and entitlement semantics.

The mission layer asks, conceptually:

```text
can subject X start mission M version V under access policy A?
```

It does not ask a store SDK directly.

Free content is represented by an explicit free/public access policy, not by the absence of all access semantics if that would make interpretation ambiguous.

## Runtime session model

A runtime mission session/run pins the exact immutable version:

```text
MissionSession
  sessionId
  missionId
  contentVersion
  contentDigest
  subject / group reference
  startedAt
  status
  taskProgress[]
  gameRef?        // when social Game semantics are used
```

The session does not silently adopt a new mission version.

A later migration may adapt the current `Game` entity to reference `MissionSession` or vice versa, but this ADR does **not** require replacing the alpha `Game` API. Additive evolution is preferred.

## Interaction with existing `Game`

Current `Game` owns runtime concerns:

- name;
- expiry;
- player/thing count limits;
- turn duration;
- participating players;
- participating `Thing` listings.

Those properties remain runtime/session semantics.

For future products:

- an official single-player Mission Pack may have a `MissionSession` with no `Game`;
- a Private Event may create one `Game` associated with a pinned MissionDefinition;
- an organization mission may create many independent sessions/games against the same immutable definition;
- a future creator-authored mission uses the same definition/session boundary.

Do not copy mission content into every `Game` row as mutable fields.

## Interaction with existing `Thing`

Current `Thing` owns captured/runtime challenge evidence:

- creator;
- image URL;
- location;
- embedding;
- guesses;
- guessed state.

A mission task may eventually:

- produce a `Thing` when a player creates a challenge as part of the mission;
- reference a `Thing` or task-evidence record for completion;
- constrain permitted evidence/location/verification policy.

Do not turn authored `MissionTaskDefinition` into a `Thing` before runtime because that would invent a creator, mutable guessed state, image evidence, and other player/session fields that do not belong to content.

## Persistence direction

The alpha Supabase/SQLDelight `Game` and `Thing` schemas remain unchanged by this ADR.

When implementation begins, prefer an **additive** persistence model:

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
  canonical_definition_json
  created_at

MissionPublication
  mission_id
  active_content_version
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

The initial implementation may store the immutable definition as versioned JSON/JSONB plus indexed metadata rather than normalizing every task into relational tables immediately. Normalize only fields that require query/index/referential behavior.

This minimizes schema churn while preserving explicit version identity.

## Client cache and offline behavior

Coordinate with #16.

A cached mission artifact is keyed by the exact tuple:

```text
(missionId, contentVersion, contentDigest)
```

Requirements:

- verify schema compatibility and digest before use;
- never replace an active session's version silently;
- preserve the definition needed to render historical/current offline progress while retention policy permits;
- reconcile publication/access state when connectivity returns;
- do not contact App Store/Play on each task transition;
- use normalized entitlement state from #104;
- fail closed for corrupted/incompatible content without destroying valid local progress;
- bound storage and cache eviction;
- treat safety-critical freshness separately from ordinary content freshness.

A future publication/access policy may require a bounded freshness token before **starting** a safety-sensitive public/commercial mission offline. That decision belongs with #16/#104/#107 rather than being hard-coded into the mission schema.

## Content updates

### Editorial-only changes

If user-visible bytes change, publish a new content version even when gameplay semantics do not. This keeps the artifact digest/history honest.

Task IDs may remain stable when the underlying objective is unchanged.

### Gameplay changes

Any target, clue semantics, geography, verification profile, scoring, completion, reward, or safety-relevant instruction change creates a new content version.

### Active sessions

Active sessions remain pinned to the version they started with unless:

- a safety pause/kill policy requires play to stop; or
- a specifically designed migration is explicitly accepted by the player/product and preserves deterministic history.

Silent mid-session migration is prohibited.

## Historical records

Completion/session history must record enough identity to remain meaningful after updates:

- `missionId`;
- `contentVersion`;
- `contentDigest` or resolvable version identity;
- completed `taskId` values;
- relevant scoring/verification profile version references where required for audit/evaluation.

Deleting or replacing a current mission must not make historical completion uninterpretable.

## Validation rules

A MissionDefinition is rejected before publication/cache use when any required invariant fails, including:

- unsupported schema version;
- blank/malformed mission identity;
- non-positive/invalid content version;
- digest mismatch;
- duplicate task IDs;
- invalid/ambiguous task order;
- zero tasks where the selected play policy requires at least one;
- invalid geographic policy;
- unsupported verification/scoring profile;
- verification profile incompatible with the required embedding schema/model contract;
- malformed access/safety policy references;
- hidden authority-only fields present in a player package where policy forbids them.

Validation must return typed failures/stable diagnostics rather than relying on UI exceptions or database errors.

## Serialization

Use a deterministic, versioned serialization contract suitable for KMP and backend validation.

Kotlin implementation should prefer `kotlinx.serialization` for the shared DTO boundary unless a deliberate alternative is justified.

Canonical-digest generation requires a documented canonical serialization representation. Ordinary JSON object key order must not be treated as stable unless canonicalization defines it.

Do not compute the content digest over platform-specific models or localized in-memory object representations.

## Security and trust boundaries

- Mission definitions are untrusted input when received from network/cache/creator tooling and must be validated.
- Publisher identity is authorization context, not proof of permission merely because a field claims it.
- Player-visible packages must not accidentally contain hidden answers or privileged anti-cheat material.
- Verification/scoring profiles are allowlisted/versioned application/backend policy.
- Safety/moderation lifecycle is server-authoritative for public/commercial publication.
- Exact location is sensitive data and should be minimized/disclosed according to mission policy.
- Content digests detect mismatch/corruption but do not replace authenticated transport, backend authorization, or signature policy.

## Consequences

### Positive

- one content primitive supports free official content, paid Mission Packs, Private Events, B2B experiences, and later creator content;
- existing alpha `Game`/`Thing` runtime models do not need a disruptive rewrite;
- immutable versions make purchases, offline caches, support, and historical completion auditable;
- publication can pause/retire unsafe content without rewriting immutable artifacts;
- store billing stays outside gameplay/domain content;
- verification remains compatible with #91 and cannot be weakened arbitrarily by content authors;
- future creator tooling can target the same declarative contract as internal content.

### Costs

- adds identity/version/publication/session concepts beyond the current simple `Game`/`Thing` model;
- requires explicit mapping between authored tasks and runtime evidence;
- requires canonical serialization/digest tooling;
- offline content needs cache/version reconciliation;
- public/commercial publishing requires operational moderation state in addition to content storage.

These costs are accepted because collapsing these concerns would create much larger migration and integrity problems once content can be purchased or published externally.

## Implementation slices

Implementation is post-alpha and should be delivered incrementally.

### Slice A — shared contract and validator

- add opaque mission/task/version/profile reference value types;
- add serializable MissionDefinition DTO/domain mapping;
- add deterministic validation and typed failures;
- add canonical serialization/digest utility;
- add common unit/property tests for schema/version/ID/order/profile invariants;
- no backend/store/UI integration.

### Slice B — immutable persistence and cache

- add additive Supabase mission/version/publication schema;
- add client cache keyed by mission/version/digest;
- validate downloaded definitions before cache/use;
- keep existing `Game`/`Thing` alpha tables intact;
- coordinate offline behavior with #16.

### Slice C — runtime MissionSession integration

- introduce pinned session/run state;
- map Private Event/team flows to existing `Game` only where needed;
- map task evidence to `Thing` or a dedicated task-evidence record deliberately;
- preserve historical version identity.

### Slice D — publishing and safety lifecycle

- implement draft/review/published/paused/retired operational state;
- add publisher authorization boundary;
- add emergency safety pause semantics from #107;
- keep full creator marketplace tooling out of scope.

## Acceptance tests for implementation

At minimum cover:

- decode/validate one representative free official mission;
- decode/validate one representative Private Event template;
- same `missionId` with two immutable content versions;
- digest mismatch rejection;
- unsupported schema rejection;
- duplicate task ID/order rejection;
- incompatible verification profile rejection;
- player package cannot contain prohibited hidden authority fields;
- active session remains pinned while a newer version is published;
- ordinary pause prevents new starts without mutating definition;
- safety pause stops/blocks according to safety policy;
- retired content remains historically resolvable;
- offline cached artifact revalidates by version/digest and reconciles later;
- existing free alpha `Game`/`Thing` behavior remains unaffected until explicit integration slices land.

## Non-goals

- App Store or Google Play billing integration;
- SKU design or pricing;
- entitlement implementation (#104);
- creator marketplace/payouts;
- organization administration portal;
- public discovery/ranking;
- replacing the current alpha `Game`/`Thing` model;
- changing #91 embedding implementation;
- changing #12/#13 clue generation/provider behavior;
- expanding #90.

## Related work

- #16 — offline-first gameplay/cache behavior.
- #18 — release-critical baseline privacy/security model.
- #90 — closed-alpha/public-beta release; this ADR is non-blocking.
- #91 — canonical embedding/verification contract.
- #103 — Mission/Adventure content-contract design issue.
- #104 — platform-neutral catalog/entitlement contract.
- #107 — post-alpha monetization/geofenced-UGC/commercial-fraud threat model.
- `docs/product/monetization.md` — product/revenue strategy that motivates this content unit.
- `docs/architecture/semantic-game-engine.md` — model/version-aware semantic and verification architecture.
