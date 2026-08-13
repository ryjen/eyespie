# ADR-0006: Product Catalog and Entitlement Contract

## Status

Accepted for post-alpha architecture.

Implementation remains explicitly outside the closed-alpha critical path in #90.

## Context

Eyespie's monetization strategy requires several ways to grant product access:

- owned Mission Packs;
- paid Private Event hosting/access;
- organization-issued access;
- promotional/support grants;
- future Passport/subscription access;
- future creator-content purchases.

ADR-0005 defines an immutable MissionDefinition with an opaque `AccessPolicyRef`. This ADR defines what satisfies that access policy without making Apple App Store or Google Play APIs part of the gameplay domain.

The current application authenticates through Supabase Auth and separately models `Player` as a gameplay/profile entity. The backend schema links a Player row to an auth `user_id`, while the shared `Player` domain model is focused on game/profile concerns. Commercial ownership must therefore attach to the authenticated account/principal, not to a mutable player profile/display identity.

The system must support cross-device restore, eventual cross-platform use where product/store policy permits it, offline play with bounded validity, refunds/revocations, and non-store grants. A client-side `isPremium` flag or direct store receipt check cannot satisfy those requirements safely.

#104 tracks this decision. #106 owns current legal/store-product classification and the eventual product policy for cross-platform use; this ADR provides the technical structure needed to express either outcome.

## Decision

Eyespie will separate five concepts:

```text
ProductDefinition
    |
    +--> channel/store Offer/Binding
    |
    +--> GrantSpec(s)
              |
              v
       server-normalized Grant
              |
              +--> Entitlement (durable/time-bounded access)
              +--> ConsumableCredit (server-side one-time benefit, when required)

EntitlementSnapshot
    |
    v
AccessEvaluator
    |
    v
AccessDecision for AccessPolicyRef
```

Store billing systems are evidence sources/adapters. They are not the product catalog, entitlement model, or gameplay authority.

## Core rules

1. **Internal product identity is store-independent.**
2. **The backend normalizes and verifies purchase/grant evidence.**
3. **Gameplay asks an access evaluator about an `AccessPolicyRef`; it never calls a store SDK directly.**
4. **Entitlements belong to an authenticated account/principal, not the `Player` profile object.**
5. **Offline access uses bounded, integrity-protected server-issued state; writable local flags are never sufficient proof.**
6. **Refund, revocation, expiry, account change, and duplicate/replayed purchase evidence have explicit semantics.**
7. **Non-store sources such as organizations/promotions use the same normalized grant model.**

## Identity model

### Account subject

Commercial access is owned by a stable authenticated principal such as the Supabase Auth user ID.

Conceptually:

```text
AccountSubject
  accountId: opaque auth principal ID
```

Do not use:

- email address as entitlement identity;
- `Player.name` or nickname;
- device ID;
- store account display data;
- a mutable local installation identifier.

`Player` may reference/display the gameplay persona associated with an account, but entitlement storage/evaluation remains account-scoped.

### Authentication requirement for paid products

The first paid-product implementation should require a stable authenticated account before completing a purchase that is expected to restore across devices or platforms.

Free/anonymous gameplay may remain possible where product policy permits it, but paid ownership must not become permanently stranded on an installation-only identity.

Any future anonymous-to-account purchase adoption flow requires an explicit migration/security design rather than guessing ownership later.

## Product catalog

A `ProductDefinition` is Eyespie's stable commercial/product concept, not a store listing.

Conceptually:

```text
ProductDefinition
  productId
  productType
  catalogRevision
  status
  presentation metadata
  grantSpecs[]
```

Initial/future-compatible product types include:

```text
MISSION_PACK
PRIVATE_EVENT
PASSPORT          // future
CREATOR_CONTENT   // future
COSMETIC          // future/secondary
```

The product type is descriptive/product policy. Access authority comes from the normalized grants produced by `grantSpecs`.

### Product ID

`ProductId` is stable across stores/channels and must not equal an Apple/Google SKU by definition.

Example:

```text
eyespie.mission.vancouver-spy
```

is an internal identity. Channel bindings may map it to unrelated Apple/Google product identifiers.

### Catalog revision

Catalog/business metadata may change without changing the identity of previously issued entitlements.

A catalog revision therefore versions catalog configuration separately from Mission content versions and store transaction identity.

Changing display copy or a store price does not rewrite existing entitlement history.

## Offers and channel bindings

Localized price/currency and purchase mechanism belong to an `Offer`/channel binding, not the core ProductDefinition.

Conceptually:

```text
ChannelBinding
  productId
  channel
  externalProductId
  region/policy metadata?
  status

Offer
  channelBinding
  localized price/presentation
  purchase availability
```

Channels may include:

- Apple App Store;
- Google Play;
- organization/manual contract;
- promotional/support grant;
- future web/marketplace channel if policy allows.

Store-provided localized price strings are presentation data, not entitlement authority and not persisted as gameplay identity.

#106 must verify which channel/purchase mechanism is allowed/required for each product type before implementation.

## Grant specifications

A product describes the normalized benefit it grants.

```text
GrantSpec
  grantKind
  scope
  duration/expiry policy
  portability policy reference
  consumption policy?
```

Representative scopes:

```text
MissionAccess(missionId, versionPolicy)
EventHostCapability(template/policy)
EventParticipantAccess(eventId)
CollectionAccess(collectionId)
CapabilityAccess(capabilityId)
```

Avoid arbitrary string permissions when a typed scope exists.

### Mission Pack ownership

By default, consumer Mission Pack ownership should target the stable logical `missionId`, not one exact `contentVersion`, so safe content corrections do not force repurchase.

The `MissionAccess` grant carries an explicit `versionPolicy`, for example:

```text
CURRENT_AND_FUTURE_ELIGIBLE_VERSIONS
SPECIFIC_VERSION_ONLY
```

The first Mission Pack experiment should normally use the first policy unless #106/product/legal constraints require otherwise.

This policy does not override publication state: an entitled user cannot start a retired/paused version merely because they own the product.

### Private Event purchases

A per-event purchase may need one-time server-side consumption rather than a permanent global entitlement.

Do not model this as a client-owned boolean.

The normalized design may use:

```text
ConsumableCredit
  creditId
  subject
  grantSpec
  quantity
  remaining
  source

consume credit -> create/bind EventHost entitlement for eventId
```

The exact store product classification/consumable semantics must be verified by #106 before implementation. The server remains authoritative for consumption/idempotency.

## Normalized entitlement

An Entitlement is an account-scoped authorization grant understood by Eyespie independent of the originating channel.

Conceptually:

```text
Entitlement
  entitlementId
  subject: AccountSubject
  scope: EntitlementScope
  source: GrantSource
  sourceGrantId
  issuedAt
  validFrom
  expiresAt?
  status
  portabilityPolicy
  revision
```

### Status

Use explicit normalized states such as:

```text
ACTIVE
EXPIRED
REVOKED
```

Do not create an entitlement for an unverified/pending store transaction. `PENDING`, cancelled purchase UI, store errors, etc. belong to purchase/verification workflow state, not to granted access.

### Grant source

Normalized source categories may include:

```text
APPLE_STORE
GOOGLE_PLAY
ORGANIZATION
PROMOTION
SUPPORT_ADMIN
SUBSCRIPTION       // future
CREATOR_MARKET     // future
```

This metadata is useful for reconciliation/audit but gameplay authorization should primarily evaluate scope/status/policy, not branch on raw store types.

## Purchase evidence and normalization

Raw purchase evidence is infrastructure/security data, not domain state.

```text
Store SDK
   |
   v
raw transaction evidence
   |
   v
backend platform verifier
   |
   v
normalized CommerceEvent
   |
   v
idempotent Grant/Entitlement update
```

Requirements:

- client receipt/transaction claims are never sufficient by themselves to grant access;
- backend verification uses the official provider verification/server-notification path appropriate to the platform;
- raw receipt/token payloads are retained only where required, with restricted access/retention;
- ordinary app logs, analytics, and diagnostics never contain raw receipts/tokens;
- provider-specific error payloads are mapped to stable normalized reason/diagnostic codes.

## Commerce event ledger and idempotency

Normalize provider events into an append-only or auditable ledger before mutating grants where practical.

Conceptually:

```text
CommerceEvent
  eventId
  provider
  providerEventKey
  account subject resolution
  product binding
  eventType
  occurredAt
  verification status
  restricted evidence reference
```

`provider + providerEventKey` must be idempotent/unique enough to prevent replay from issuing duplicate benefits.

Event types may include:

- PURCHASE_VERIFIED;
- RENEWAL_VERIFIED;
- REFUND;
- REVOKE;
- EXPIRE;
- RESTORE/RECONCILE.

Provider-specific naming remains inside adapters.

## Restore and reconciliation

### Same platform/device replacement

Restore/reconciliation obtains current store evidence, sends it through backend verification, and reconstructs the normalized entitlement state for the authenticated account.

### Cross-platform

The architecture supports account-wide or source-platform-restricted access without embedding the decision into store adapters.

```text
PortabilityPolicy
  ACCOUNT_WIDE
  SOURCE_PLATFORM_ONLY
  POLICY_DEFINED
```

#106 owns the product/legal decision for each commercial SKU. The entitlement contract merely preserves and enforces it.

### Backend as normalized source of truth

Once store evidence is verified, the backend normalized entitlement state is the source used by both platforms for application access decisions.

A device may trigger provider restore/reverification, but Android must not need Apple APIs and iOS must not need Google APIs to understand an already normalized account entitlement.

## Refunds, revocations, expiry, and chargebacks

A verified provider/admin event updates normalized state idempotently.

Default fail-safe policy:

- revoked/expired access cannot start new protected content;
- an active session follows its product/session policy for immediate stop versus bounded completion/grace;
- offline clients may temporarily retain access only until the signed/cached authorization validity expires;
- reconnect/revalidation corrects stale state;
- history is retained for audit/support according to retention policy rather than deleting the entitlement row and losing provenance.

Do not erase prior purchase history merely to express current denial.

## Access-policy evaluation

ADR-0005 places an opaque `AccessPolicyRef` on Mission content. This ADR resolves it through application-owned policy.

Conceptually:

```text
AccessContext
  subject
  accessPolicyRef
  mission/session context
  platform/channel context if policy requires
  current time
  entitlement snapshot

AccessDecision
  ALLOW | DENY | REFRESH_REQUIRED
  reasonCode
  decisionValidUntil?
```

`AccessEvaluator` must be deterministic/testable and independent of UI/store SDKs.

Representative reason codes:

- FREE_ACCESS;
- ENTITLEMENT_ACTIVE;
- NO_ENTITLEMENT;
- ENTITLEMENT_EXPIRED;
- ENTITLEMENT_REVOKED;
- WRONG_SUBJECT;
- SOURCE_PLATFORM_RESTRICTED;
- SNAPSHOT_EXPIRED;
- POLICY_UNSUPPORTED.

Do not expose raw provider errors as gameplay policy.

## Offline entitlement snapshots

Offline paid access cannot rely on a writable database boolean alone.

The backend should issue a bounded, integrity-protected snapshot/envelope representing the account's normalized grants required for offline decisions.

Conceptually:

```text
EntitlementSnapshot
  subject
  issuedAt
  refreshBy / expiresAt
  grants[]
  policyVersion
  signature / integrity envelope
```

The preferred implementation is a server-signed envelope verifiable by the client with a public verification key or equivalent reviewed integrity mechanism.

Properties:

- no backend signing secret is embedded in the app;
- subject/account is bound into the signed payload;
- expiry is bounded by policy;
- replay after refund is limited to the accepted offline window;
- cached state is namespaced by account and purged/invalidated on account switch;
- signature/digest failure fails closed;
- the snapshot contains normalized grants, not raw receipts or store credentials.

The exact cryptographic format/key-rotation mechanism should be selected in an implementation ADR if it is not already covered by reusable project infrastructure.

## Offline session policy

Coordinate with #16 and ADR-0005.

Before starting protected content offline:

1. exact MissionDefinition version/digest must be valid/cached;
2. the Mission publication state must satisfy the bounded freshness policy applicable to that content;
3. an unexpired entitlement snapshot must authorize the `AccessPolicyRef`;
4. verification/model/runtime prerequisites must be available.

An already-started session may have a different bounded continuation/grace policy, but that policy is explicit and testable.

Do not query App Store/Play on every mission task transition.

## Account switching

Entitlement/cache state is strictly subject-scoped.

On authenticated account change:

- do not expose the previous account's entitlements;
- namespace or purge cached entitlement snapshots;
- re-evaluate current protected sessions;
- preserve non-sensitive local history only according to account/data policy;
- never infer that two accounts are the same from shared device/store login alone.

## Organization and promotional grants

Non-store grants use the same normalized model.

An organization/admin operation may issue/revoke an entitlement directly through an authorized backend path without fabricating a fake store transaction.

Requirements:

- least-privilege grant/revoke permissions;
- explicit issuer/source identity;
- bounded audit metadata;
- idempotent grant identifiers;
- optional expiry/scope restrictions;
- no client-authoritative admin grant path.

## Product/catalog and Mission relationship

Keep these identities independent:

```text
ProductId        // commercial product
MissionId        // logical content identity
MissionVersion   // immutable content revision
AccessPolicyRef  // rule required to start/use content
Entitlement      // account grant satisfying policy
```

One product may grant multiple missions/capabilities, and one mission may be reachable through multiple products/grant sources.

Do not assume a 1:1 SKU-to-Mission mapping in domain types.

## Persistence direction

Prefer additive backend tables/collections equivalent to:

```text
Product
ProductChannelBinding
GrantDefinition / ProductGrantSpec
CommerceEvent
Entitlement
ConsumableCredit     // only when needed
```

Sensitive raw provider evidence should use a restricted storage boundary rather than ordinary client-readable entitlement rows.

RLS/authorization requirements:

- users may read only normalized entitlements for their own account unless a deliberate group/organization policy grants more;
- users cannot directly insert/update active entitlements;
- provider-verification/service paths are server-controlled;
- admin/organization grant paths use narrowly scoped capabilities and auditable operations;
- store channel bindings/catalog configuration are not client-writable.

## Client architecture

Shared application/domain code owns:

```text
ProductCatalogRepository
EntitlementRepository
AccessEvaluator
EntitlementSnapshotVerifier
```

Exact interfaces may differ, but infrastructure adapters own:

- StoreKit/App Store APIs;
- Google Play Billing APIs;
- provider transaction objects;
- backend verification calls;
- secure/restricted transaction evidence handling.

UI consumes normalized catalog/offers/access state rather than importing billing SDK state across the application.

## Privacy and telemetry

Do not include in product analytics or ordinary logs:

- raw receipts;
- transaction tokens;
- store account identifiers;
- full provider payloads;
- payment instrument data;
- auth tokens;
- signed offline snapshot payloads when they contain account/grant details.

#105 may record bounded normalized states such as purchase flow started/completed/failed reason class, product type/internal product ID where acceptable, and entitlement decision category according to its allowlist.

#107 owns monetization/fraud threat-model extensions.

## Failure policy

Fail closed for:

- invalid/unverified purchase evidence;
- subject mismatch;
- malformed/unsupported entitlement scope;
- invalid signed snapshot;
- expired snapshot when refresh is required;
- forged/replayed admin/organization grant event;
- unsupported access-policy version.

Gameplay unrelated to protected content should remain available when the entitlement subsystem is degraded.

A billing/backend outage must not crash startup or make free gameplay unusable.

## Consequences

### Positive

- App Store/Play APIs remain replaceable infrastructure rather than game-domain dependencies;
- one normalized model supports purchases, organization access, promotions, subscriptions, and future creator content;
- cross-device/platform restore can use account-level backend state;
- offline paid content is possible without trusting writable local flags;
- refund/revocation and duplicate-event behavior are explicit;
- Mission content does not need price/SKU/store knowledge;
- testing can use fake catalog/entitlement repositories without store credentials.

### Costs

- requires backend transaction verification and normalized state;
- requires account identity before durable paid ownership;
- requires idempotent commerce event processing;
- offline access requires signed/bounded snapshots and key lifecycle management;
- Private Event one-time purchases may require a server-side credit/consumption ledger;
- cross-platform policy must be maintained separately from provider adapters.

These costs are accepted because directly trusting client store state or hard-coding SKUs into gameplay creates materially worse integrity, restore, offline, and portability problems.

## Implementation slices

Implementation remains post-alpha.

### Slice A — shared catalog/access contract

- internal ProductId/ProductDefinition/GrantSpec types;
- normalized Entitlement/Scope/Status types;
- AccessPolicyRef resolution and deterministic AccessEvaluator;
- fake/no-op repositories for common tests;
- no store SDKs or backend purchase verification yet.

### Slice B — backend normalized entitlement service

- additive Product/channel binding/CommerceEvent/Entitlement persistence;
- account-subject binding;
- server-owned mutation and RLS;
- idempotent grant/revoke/reconcile operations;
- organization/promotional grant path;
- no Apple/Google adapter until #106 confirms product classifications.

### Slice C — offline signed snapshot

- server-issued bounded entitlement snapshot;
- client verification/key rotation strategy;
- account namespacing and switch behavior;
- #16 integration and failure-path tests.

### Slice D — first store adapter(s)

After #106 verifies the purchase mechanism for the selected first paid experiment:

- implement the minimum Apple/Google adapter(s);
- server-verify provider evidence;
- process provider notifications/refunds/revocations;
- restore/reconcile normalized entitlements;
- keep provider types out of shared gameplay APIs.

### Slice E — Private Event consumption if required

Only if the selected Private Event purchase model needs one-time use:

- server-side consumable credit;
- atomic/idempotent consumption;
- derived event-host entitlement;
- refund/race/failure semantics.

## Acceptance criteria for implementation

At minimum verify:

- internal ProductId differs from Apple/Google external IDs;
- one Mission Pack product grants MissionAccess for a logical MissionId;
- a second grant source (organization/promotion) grants equivalent scope without fake store data;
- replayed purchase event does not duplicate entitlement/credit;
- refund/revoke prevents new access after reconciliation;
- account switching cannot leak prior account entitlements;
- invalid local writable state cannot grant protected access;
- signed snapshot allows offline access until bounded expiry and rejects tampering;
- expired snapshot returns `REFRESH_REQUIRED`/deny according to policy;
- free gameplay remains usable when billing/entitlement infrastructure is unavailable;
- store SDK/provider types remain infrastructure-only.

## Non-goals

- selecting prices;
- creating production store SKUs before #106;
- implementing arbitrary payment-provider abstraction;
- creator payouts/marketplace;
- full subscription/Passport implementation;
- B2B billing/invoicing platform;
- changing the MissionDefinition identity model from ADR-0005;
- expanding #90.

## Related work

- #16 — offline-first behavior.
- #90 — release; entitlement implementation is non-blocking.
- #103 / ADR-0005 — Mission content identity and AccessPolicyRef boundary.
- #104 — product catalog/entitlement design issue.
- #105 — privacy-preserving product analytics.
- #106 — legal/store product classification and cross-platform commercial policy.
- #107 — monetization/geofenced-UGC/commercial-fraud threat model.
