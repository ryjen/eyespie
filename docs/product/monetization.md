# Eyespie Monetization Strategy

## Status

Product strategy and post-alpha architecture guidance.

This document does **not** expand the closed-alpha scope tracked by #90. The immediate release objective remains proving the complete two-player game loop on physical Android and iOS devices. Monetization implementation begins only after the core loop is usable and measurable.

## Product thesis

Eyespie should monetize **playable real-world experiences and hosting**, not access to the basic social loop.

The recommended model is:

1. free core social play;
2. paid Mission/Adventure packs;
3. paid Private Events;
4. hosted commercial experiences for organizations;
5. an optional Passport only after recurring official content has demonstrated value;
6. a creator marketplace only after authoring, moderation, safety, payments, and content quality are mature;
7. cosmetics only as secondary, non-gameplay-affecting revenue.

Behavioral advertising, pay-to-win mechanics, artificial verification advantages, and a mandatory early subscription are explicitly out of scope.

## Monetizable unit: Mission / Adventure

The durable product unit should be a versioned, declarative real-world mission rather than hard-coded app behavior.

A mission may contain:

- immutable identity and schema version;
- title, description, theme, story, and presentation metadata;
- geographic constraints or bounded play area;
- one or more tasks / things to find;
- clues and expected-answer data with appropriate disclosure boundaries;
- machine-vision verification policy and compatible embedding/model identity;
- scoring, progression, completion conditions, and rewards;
- solo/team rules;
- availability window and lifecycle state;
- safety, accessibility, age, and moderation metadata;
- entitlement requirements;
- content provenance and publisher identity.

Expected lifecycle:

```text
draft -> review -> published -> paused -> retired
```

Real-world locations change. Published paid content must be updateable, pausable, and retireable without silently breaking player purchases or completion history.

## Revenue models

### Mission Packs

Mission Packs are the preferred first consumer monetization experiment. They should package a bounded, understandable experience such as a city mission, nature hunt, road-trip challenge, seasonal hunt, photography challenge, or family travel pack.

Pricing is intentionally experimental. Early tests should optimize for evidence of willingness to pay and completion, not revenue extraction.

### Private Events

Private Events are a lightweight prosumer product for birthdays, road trips, schools, weddings, family groups, community events, and corporate outings.

The paid value is hosting and coordination:

- invite-only access;
- host controls;
- custom title/theme/clues/area;
- team assignment;
- event leaderboard;
- bounded participant count and event duration;
- optional completion summary or badges.

This tests whether users will pay for a hosted experience without requiring a subscription.

### Hosted B2B experiences

B2B may monetize before consumer scale is large. Candidate customers include tourism organizations, hotels, museums, attractions, municipalities, shopping districts, conferences, festivals, schools, camps, and team-building providers.

Initial delivery should be high-touch and intentionally manual. Build organization self-service only after several customers repeatedly request the same operational capability.

Likely organization capabilities include:

- organization identity and roles;
- mission cloning/templates;
- branding controls;
- participant limits;
- scheduling/expiration;
- aggregate, privacy-preserving completion analytics;
- content moderation/safety review;
- administrative override and support tooling.

### Eyespie Passport

Do not introduce a subscription merely to monetize existing free functionality.

A Passport is justified only when Eyespie can reliably provide recurring value through a growing official/seasonal mission catalog, enhanced group/private-game capabilities, offline travel content, or similar durable benefits.

### Creator marketplace

The mission model should be compatible with creator-authored content, but a public marketplace is deliberately deferred.

A marketplace introduces material new systems and risks:

- creator identity and payouts;
- tax/payment handling;
- moderation and IP complaints;
- physical-location safety review;
- duplicate/low-quality content;
- refunds when locations change;
- fraud and manipulated verification;
- ratings, ranking, discovery, and abuse controls.

## Core product principles

### Protect the social loop

A player should not need to pay merely to join or complete a normal challenge shared by a friend. Charging at the invitation boundary directly taxes acquisition and network effects.

### Sell experiences, not frustration

Payments may unlock meaningful content, hosting, customization, or organization capabilities. They must not change verification thresholds, scoring fairness, leaderboard authority, or other competitive outcomes.

### Prefer privacy-preserving economics

Eyespie processes camera and location data. Avoiding behavioral advertising removes a substantial privacy, consent, SDK, supply-chain, and trust surface. Prefer on-device ML where accuracy permits and measure cloud inference as a direct unit cost when it is required.

### Keep billing outside the domain model

App Store and Play identifiers are adapters, not product identities.

The domain should distinguish:

```text
Product catalog -> Entitlement -> Game capability/content
                       ^
                       |
          App Store / Play / organization grant /
          promotion / future subscription / creator purchase
```

The entitlement layer should eventually support:

- owned Mission Pack;
- Private Event access;
- organization grant;
- promotional grant;
- future Passport access;
- future creator purchase.

Store-specific SKU identifiers, receipt formats, and billing APIs must remain outside core game/domain types.

## Offline and entitlement behavior

Paid content should remain usable offline where the product promise permits it.

Offline design should define:

- locally cached mission definitions and assets;
- bounded cached-entitlement validity;
- reconciliation when connectivity returns;
- behavior after refund, revocation, expiration, or account changes;
- update/retirement behavior for previously downloaded missions;
- failure semantics when store services are unavailable;
- no requirement to contact a store service during each gameplay step.

This belongs with the offline-first architecture tracked by #16.

## Security, safety, and abuse implications

Monetization creates incentives that do not exist in a purely casual game. The threat model in #18 must cover at least:

- GPS/location spoofing;
- replayed or canned photos;
- manipulated embeddings or verification inputs;
- account farming and fake completion;
- unsafe/trespassing mission locations;
- stalking or sensitive-location leakage;
- abusive geofenced UGC;
- paid-content copying;
- refund/chargeback abuse;
- future creator payout fraud;
- moderation, appeals, and content retirement.

Geofenced UGC is a trust-and-safety system, not merely a content-authoring feature.

## Analytics and unit economics

Product analytics should answer whether the free loop works before optimizing monetization.

Core funnel:

```text
install
 -> first challenge joined
 -> first challenge completed
 -> challenge created
 -> invite sent
 -> friend completes
 -> return / repeat creation
```

Paid-product metrics may later include:

- mission detail view -> purchase conversion;
- purchased -> started -> completed;
- repeat purchase;
- Private Event purchase and repeat hosting;
- B2B pilot-to-paid conversion;
- time to create/deploy a commercial mission;
- support cost per commercial engagement;
- on-device/cloud verification ratio;
- inference, storage, egress, moderation, refund, and payment cost per completed mission.

Analytics must not collect raw images, embeddings, exact locations, hidden answers, arbitrary prompts, tokens, or ad-tech identifiers.

## Licensing and commercial distribution

Eyespie currently uses GPLv3. Commercial use and charging are compatible with GPLv3, but commercial distribution must preserve applicable source and redistribution rights.

Do not rely on a proprietary mobile client fork as the business moat. Prefer defensible value in:

- official content/catalog;
- hosted services;
- organization operations;
- brand/trademark;
- moderation and trust systems;
- creator ecosystem;
- commercial relationships.

Before commercial store launch, resolve:

- GPLv3 compatibility with the intended Apple/Google distribution path;
- copyright ownership and contributor provenance;
- third-party dependency/license obligations;
- whether an additional permission, exception, or other licensing strategy is appropriate;
- trademark and brand policy;
- mission/content licensing terms;
- classification of digital content, hosted services, and real-world services under current store rules;
- cross-platform entitlement and purchase-restoration behavior.

These are commercial-launch gates, not closed-alpha gates.

## Delivery sequence

### Phase 0 — prove and instrument the free loop

Complete #90 and the supporting release blockers. Add only the privacy-preserving measurements needed to understand activation, completion, creation, invitation, and retention.

### Phase 1 — first paid experiments

Implement the versioned Mission/Adventure contract and platform-neutral entitlement model. Test a small number of high-quality Mission Packs and Private Events.

### Phase 2 — sell B2B manually

Use lightweight organization tooling and high-touch delivery to operate a few real commercial missions. Capture repeated needs before building self-service administration.

### Phase 3 — productize recurring value

If paid content shows repeat usage and retention value, consider Passport and productize the organization capabilities proven in Phase 2.

### Phase 4 — open the platform

Only after mission authoring, safety, moderation, and payments are mature should Eyespie expose creator publishing and marketplace economics.

## Decision gates

- Add more Mission Packs only when paid-pack completion and repeat purchase demonstrate durable content value.
- Build deeper Private Event tooling only when hosts repeatedly run additional events.
- Build B2B self-service only when multiple customers request the same operational capabilities.
- Launch Passport only when recurring official content demonstrates retention value.
- Launch a creator marketplace only when internal and commercial authors can already create and operate safe, high-quality missions reliably.

## Related work

- #90 — release Eyespie closed alpha/public beta; monetization must not expand its critical path.
- #103 — versioned Mission/Adventure content contract; first post-alpha architecture dependency.
- #104 — platform-neutral catalog/entitlement contract; consumes #103.
- #105 — privacy-preserving product analytics and unit economics; separate from release diagnostics.
- #106 — commercial distribution, licensing, and store-product boundary; commercial-launch gate.
- #16 — offline-first gameplay and cached entitlement behavior.
- #18 — privacy/security/threat model, including monetization-driven abuse cases.
- #91 — canonical image-embedding and verification contract used by mission verification policy.
- #12 / #13 — clue schema/provenance and GenAI routing boundaries.
- #93 — release observability; product analytics should remain a separate concern.
