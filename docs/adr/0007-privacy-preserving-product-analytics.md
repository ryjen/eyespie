# ADR-0007: Privacy-Preserving Product Analytics and Unit-Economics Measurement

## Status

Accepted for post-alpha architecture.

Implementation remains explicitly outside the closed-alpha critical path in #90.

## Context

Eyespie needs evidence that the free social loop works before optimizing monetization. The product strategy asks questions such as:

- do players join and complete a first challenge;
- do they create/share a challenge afterward;
- does an invited friend complete it;
- do players return;
- do paid Mission Packs get purchased, started, completed, and repurchased;
- do Private Event hosts run another event;
- what fraction of verification is on-device versus remote;
- what does a completed mission cost to serve.

#93 separately owns release observability: crash/error reporting, release identity, stable diagnostic codes, and coarse release-critical stage transitions. Product analytics must not become a second crash/logging system or a way to collect arbitrary debug payloads.

Eyespie also handles camera input, embeddings, location, clues/hidden answers, social relationships, and purchase state. A generic `Map<String, Any>` analytics SDK surface would make it too easy for sensitive data to enter telemetry.

Bluebell is the reusable KMP architecture foundation, but no existing analytics abstraction was found that should be adopted as the product policy boundary. Eyespie therefore owns the event taxonomy/policy initially. A generic event-sink primitive may be extracted to Bluebell later only after reuse is demonstrated; Eyespie-specific event names, privacy policy, funnel semantics, and data classification remain product-owned.

## Decision

Eyespie will use **typed, allowlisted, first-party product events** behind an injectable application-owned sink.

The initial architecture separates four concerns:

```text
Domain/application behavior
        |
        v
Typed ProductEvent
        |
        v
ProductAnalytics sink
        |
        +--> NoOp / Fake
        +--> bounded first-party queue
                    |
                    v
             Eyespie backend ingestion
                    |
                    v
             product-event store/aggregates

CommerceEvent (#113) -----------------> revenue/conversion truth
Server/provider usage ----------------> UsageMeter / cost truth
#93 diagnostics ----------------------> release reliability truth
```

Product analytics is not the source of truth for purchases/revenue or cloud billing cost. Verified commerce and server metering feed those metrics from their authoritative systems.

## Principles

1. **Typed events, not arbitrary property maps.**
2. **Default deny for new properties.** A new field requires schema/privacy review.
3. **No behavioral advertising, attribution SDK, cross-app tracking, fingerprinting, or session replay.**
4. **First-party ingestion first.** Avoid third-party mobile analytics SDKs for the initial implementation.
5. **Free gameplay never depends on analytics availability.** Events are best-effort and bounded.
6. **Do not use analytics as a hidden debug/log channel.** #93 owns diagnostics.
7. **Do not derive revenue from client purchase callbacks.** #113/#115 verified commerce is authoritative.
8. **Do not derive cloud cost from client guesses.** Server/provider metering is authoritative.
9. **Minimize identifiers and correlation scope.** Correlate only what is needed to answer an approved product question.
10. **Retention/deletion/access are part of the event contract, not an afterthought.**

## Explicit prohibited data

Product analytics must not contain:

- raw images, thumbnails, frame bytes, or image filenames/paths;
- embeddings or vector-derived payloads;
- exact latitude/longitude, geofence coordinates, movement trails, or raw addresses;
- hidden answers;
- arbitrary clue text, prompts, model responses, OCR text, or scene descriptions;
- contact details, invite recipient identity, phone/email/address-book data;
- auth tokens, API keys, signed URLs, credentials, cookies;
- raw App Store/Play receipts, transaction tokens, provider payloads, payment-instrument data;
- private filesystem paths;
- advertising identifiers;
- device fingerprint material;
- arbitrary environment dumps, stack traces, or log messages;
- user-generated mission/challenge titles or free-form text;
- raw account/user/player IDs in third-party or analytics-facing event payloads.

Exact location and user-generated content are prohibited even when convenient for segmentation. Use approved coarse/categorical dimensions instead.

## Event model

Use a closed/typed event hierarchy rather than generic names/properties.

Conceptually:

```text
ProductEventEnvelope
  schemaVersion
  eventId
  occurredAt
  releaseIdentity
  platform
  environment
  correlationContext?
  event: ProductEvent

ProductEvent
  FirstRun
  ChallengeJoined
  ChallengeCompleted
  ChallengeCreated
  ShareInitiated
  SharedChallengeCompleted
  SessionReturned
  MissionCatalogViewed
  PurchaseFlowStarted
  PurchaseFlowCancelledOrFailed
  MissionStarted
  MissionCompleted
  PrivateEventCreated
  PrivateEventStarted
  PrivateEventCompleted
  VerificationCompleted
  ...bounded additions only
```

Each concrete event has a fixed typed property set. Do not provide a public API such as `record(name: String, properties: Map<String, Any?>)` to ordinary feature code.

If a vendor adapter later requires string/maps internally, conversion occurs only inside the adapter from the reviewed typed event model.

## Event versioning

There are two versions:

- envelope/schema version for transport shape;
- semantic version/name revision for an event when metric meaning changes materially.

Do not silently redefine an existing event.

If `ChallengeCompleted` changes from “client showed completion UI” to “backend committed completion,” create a new semantic version or event rather than corrupting longitudinal analysis.

## Core free-loop funnel

The initial approved product questions map to a minimal funnel:

```text
first run
 -> first challenge joined
 -> first challenge completed
 -> challenge created
 -> share/invite initiated
 -> downstream shared challenge completed
 -> return / repeat completion or creation
```

The first implementation should prefer **successful domain/backend transitions** over UI button taps where practical.

Examples:

- `ChallengeCreated` means creation persisted/accepted, not merely that the create screen opened.
- `ChallengeCompleted` means completion/match outcome reached the accepted product state.
- UI attempts may be measured only when needed to diagnose funnel abandonment and must use bounded reason categories.

## Correlation and identity

### Default

Do not send raw auth account IDs, Player IDs, email, device IDs, or platform advertising identifiers as analytics identity.

### Authenticated account-level metrics

When retention/repeat-use metrics require account-level correlation, derive an **analytics-specific pseudonymous subject** on the trusted backend from the authenticated account principal.

Recommended shape:

```text
analyticsSubject = HMAC(analytics-secret-version, authAccountId)
```

Properties:

- secret stays server-side;
- analytics payload never needs the raw account ID;
- subject version is explicit for key rotation;
- deletion tooling can recompute the pseudonym for the authenticated account;
- this identifier is first-party only and is not shared for advertising/attribution.

Do not generate this HMAC in the mobile client.

If stable account correlation is not required for a metric, prefer session/event-level aggregation instead.

### Unauthenticated use

Use ephemeral session correlation only. Do not manufacture a durable fingerprint to track anonymous users across reinstalls/devices.

Do not automatically stitch anonymous history to an account unless a later explicit privacy/product decision requires it.

## Share/invite correlation

To measure the social loop without storing recipient identity, use a single-purpose opaque share correlation identifier or server-side relationship where available.

Requirements:

- no recipient email/phone/contact value in analytics;
- bounded TTL where the identifier exists solely for analytics;
- no use as a general tracking identifier;
- completion can be aggregated as “shared challenge produced downstream completion” without exposing who completed it.

If the platform share sheet provides no reliable downstream correlation, accept that metric gap rather than adding invasive contact tracking.

## Content/product dimensions

### Safe default dimensions

Prefer categorical fields such as:

- free challenge vs official Mission vs Private Event;
- official/organization/user-hosted publisher class;
- local vs remote verification;
- success/failure normalized reason class;
- platform/app release;
- coarse mission category/theme where explicitly curated and non-sensitive;
- paid/free access class.

### Mission/product IDs

Internal ProductId may be included for a small allowlisted set of official Eyespie products when required for conversion analysis.

Do not send arbitrary UGC/creator MissionId/ChallengeId as a generic analytics dimension by default. An opaque ID can still become a durable proxy for a sensitive place/person/community. Prefer category/aggregate metrics or transform on the trusted backend.

## Location policy

Product analytics does not receive precise location.

If geographic market analysis later becomes necessary, use only a separately reviewed coarse region derived on a trusted boundary with explicit minimum granularity/k-anonymity policy. Do not derive/store movement trails.

The initial implementation does not require geographic analytics.

## Purchase and revenue truth

Client events may measure purchase UX:

- offer/catalog viewed;
- purchase flow started;
- user cancelled;
- normalized non-sensitive failure class.

They do **not** prove revenue.

Revenue, refunds, revocations, and paid conversion use verified normalized CommerceEvent/Entitlement state from ADR-0006/#113/#115.

This prevents client replay, duplicate callbacks, and unverified purchase UI from distorting business metrics.

## Verification and unit economics

Separate behavioral product events from technical cost metering.

### Client/on-device signal

A typed `VerificationCompleted` event may contain only bounded fields such as:

```text
execution: LOCAL | REMOTE | RULES
resultClass: MATCH | NO_MATCH | ABSTAIN | FAILED
latencyBucket
verificationProfileVersion
```

Do not include embedding values, image identifiers, exact model input, or raw exception text.

### Server/provider metering

Remote/provider cost metrics come from a server-side `UsageMeter`/provider billing boundary:

- provider/model/config identity where non-sensitive;
- request count;
- token/compute units where applicable;
- storage/egress bytes where attributable;
- normalized success/failure;
- cost inputs/rates outside the mobile app.

Approximate cost per completed Mission is calculated in reporting by joining aggregate product outcomes with authoritative server usage, not by placing cloud prices in the client.

## B2B metrics

Commercial pilot metrics such as:

- qualified leads;
- pilot-to-paid conversion;
- contract/revenue value;
- time to create/deploy a client mission;
- support interventions;
- repeat campaigns;

belong to business/operations reporting, not mobile telemetry.

The app/backend may emit aggregate participant started/completed counts where privacy policy permits, but no B2B customer dashboard is part of this ADR.

## Architecture and dependency boundary

Shared/application code owns an interface such as:

```kotlin
interface ProductAnalytics {
    fun record(event: ProductEvent)
}
```

The exact method may be suspend/batched according to implementation needs, but behavior is:

- injectable;
- no-op implementation is first-class;
- deterministic fake captures events for tests;
- analytics failure is swallowed/mapped outside gameplay result paths;
- event schema is centrally reviewed;
- feature code cannot add arbitrary properties.

### Bluebell boundary

Eyespie owns:

- `ProductEvent` taxonomy;
- property allowlist/prohibited-data policy;
- identity/correlation policy;
- funnel semantics;
- retention/deletion requirements;
- monetization/unit-economics meaning.

Bluebell may later own only generic reusable plumbing such as a typed event sink, bounded queue primitive, or lifecycle-safe batching abstraction if another product proves reuse. Do not move Eyespie event semantics or data policy into Bluebell.

## First-party ingestion

Prefer an Eyespie-controlled backend endpoint/service for the first implementation.

Advantages:

- no mobile ad/analytics SDK supply-chain surface;
- authenticated server can derive pseudonymous subject safely;
- server can reject unknown event schema/fields;
- data deletion/access policy remains under project control;
- easier joining with authoritative server commerce/usage aggregates without sending sensitive provider data to clients.

The ingestion endpoint must treat event payloads as untrusted input and revalidate schema/size/property constraints server-side.

## Local buffering

Offline/batched analytics may use app-private bounded storage.

Requirements:

- bounded by count and/or bytes;
- analytics queue cannot grow without limit;
- no protected content is added merely because queue storage is local;
- failed upload does not block gameplay;
- account-correlated queued events are isolated/purged according to logout/account-switch policy;
- duplicate delivery is tolerated through event IDs/idempotent ingestion;
- backoff/retry is bounded;
- low-storage conditions may drop analytics rather than gameplay data.

Prefer dropping analytics over degrading core play.

## Server ingestion and validation

The server accepts only supported envelope/event versions and fixed typed property schemas.

Reject/drop with bounded diagnostics when:

- unknown event version;
- oversized payload;
- unknown/disallowed field;
- malformed timestamp/ID;
- prohibited free-form field;
- invalid environment/release metadata.

Do not echo rejected payload content into logs.

## Retention, access, deletion, and governance

The implementation must publish a data-governance note before production activation covering:

- raw event retention period;
- aggregate retention period;
- analytics-subject derivation/key rotation;
- who/which service roles can query raw events;
- development/staging/production separation;
- account deletion/erasure path;
- procedure for adding/changing an event/property;
- audit/review of exports and dashboards;
- backup/retention implications.

Default direction:

- keep raw pseudonymous event retention bounded to the shortest period useful for funnel analysis;
- retain longer-lived aggregates only when they no longer require person-level correlation;
- default-deny new fields;
- no production raw-event access for ordinary application roles.

Do not hard-code a long retention period in client code.

## Consent / settings

This ADR does not assume behavioral-ad consent because advertising/tracking is prohibited.

If applicable privacy policy/law/product positioning requires analytics opt-out or consent, implement it as application policy with no-op sink behavior and server enforcement where appropriate. #18/#107 review the selected production policy.

Do not design analytics so core gameplay requires consent.

## Interaction with #93 diagnostics

#93 and this ADR may share release identity and stable coarse reason enums, but they remain distinct channels.

Do not put into product analytics:

- stack traces;
- arbitrary exception messages;
- private paths;
- raw environment values;
- crash dumps;
- verbose diagnostic payloads.

Likewise, do not use crash reporting to reconstruct behavioral funnels.

## Testing

Required test surfaces include:

- event type can serialize only approved properties;
- prohibited fields have no representation in typed DTOs;
- server rejects unknown/disallowed/oversized payloads;
- NoOp never affects gameplay;
- Fake captures exact expected events in domain/use-case tests;
- failure/timeout of analytics adapter does not change feature outcome;
- duplicate upload is idempotent;
- account switching isolates queued/correlated events;
- free-loop transitions emit success events only at the defined semantic boundary;
- commerce metrics use verified CommerceEvent rather than client purchase success;
- server usage metering is separated from client ProductEvent.

Negative tests should specifically attempt to add raw image/location/answer/receipt/token/free-form content and prove the normal event contract cannot serialize it.

## Consequences

### Positive

- measures product value without ad-tech/data-maximization incentives;
- event policy is reviewable in code;
- privacy-sensitive fields are structurally harder to add accidentally;
- first-party backend can derive pseudonyms without exposing raw account IDs;
- verified commerce and server usage remain authoritative for money/cost;
- Bluebell remains generic instead of inheriting Eyespie product policy;
- analytics failure cannot break play.

### Costs

- typed events require deliberate schema updates;
- first-party ingestion/reporting requires some backend/data operations;
- some convenient segmentation/correlation is intentionally unavailable;
- social attribution may be incomplete when privacy-safe correlation is impossible;
- deletion/retention governance must be operated, not just coded.

These costs are accepted because broad third-party analytics or arbitrary event maps would create a disproportionate privacy/security/supply-chain surface for a camera/location/social product.

## Implementation slices

### Slice A — typed event contract and test seam

- ProductEvent hierarchy/envelope;
- property allowlist/prohibited-data documentation;
- ProductAnalytics interface;
- NoOp + Fake implementations;
- common serialization/negative tests;
- no network/backend dependency.

### Slice B — first-party ingestion and bounded queue

- authenticated/batched ingestion endpoint;
- backend schema/size validation;
- pseudonymous subject derivation where approved;
- bounded client queue + idempotent event IDs;
- retention/access/deletion governance document;
- environment separation.

### Slice C — free-loop instrumentation

Instrument only the minimum activation/create/share/complete/return transitions, at domain/backend success boundaries where possible. Validate resulting funnel before adding event breadth.

### Slice D — commerce and unit-economics integration

After #113/#115 and relevant verification paths exist:

- derive paid conversion/revenue/refunds from verified commerce;
- add bounded purchase-UX events only where useful;
- integrate client local/remote verification classification;
- integrate server/provider UsageMeter;
- calculate unit economics in reporting/aggregation rather than client code.

## Non-goals

- behavioral ads/attribution;
- third-party mobile analytics SDK as a requirement;
- cross-app tracking/fingerprinting;
- session replay/screen recording;
- precise location analytics;
- generic logging/crash reporting (#93);
- data warehouse/BI platform;
- B2B customer dashboards;
- creator marketplace analytics;
- expanding #90.

## Related work

- #18 — baseline privacy/security model.
- #90 — release; product analytics implementation is non-blocking.
- #93 — release diagnostics/observability.
- #105 — product analytics/unit-economics design issue.
- ADR-0005/#103 — Mission content/session identity.
- ADR-0006/#104 — catalog/entitlement architecture.
- #107 — monetization/commercial-fraud threat model.
- #113/#115 — authoritative verified commerce source.
- `docs/product/monetization.md`.
