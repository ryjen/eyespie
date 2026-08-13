# Product Catalog and Entitlement Architecture

The normative decision is [ADR-0006](../adr/0006-product-catalog-and-entitlement-contract.md).

This document provides the concise implementation map for the post-alpha catalog/entitlement capability designed in #104.

## Boundary

```text
ProductDefinition
    |
    +--> Store/channel binding + localized Offer
    +--> GrantSpec(s)
             |
             v
        verified/normalized Grant
             |
             +--> Entitlement
             +--> optional one-time ConsumableCredit

EntitlementSnapshot
    |
    v
AccessEvaluator
    |
    v
AccessDecision for Mission AccessPolicyRef
```

The key rule is: **store APIs provide evidence; they do not define gameplay ownership.**

## Identities stay separate

```text
ProductId        // Eyespie commercial identity
MissionId        // logical content identity from ADR-0005
MissionVersion   // immutable content revision
AccessPolicyRef  // content access requirement
AccountSubject   // authenticated commercial owner
Entitlement      // normalized grant satisfying access policy
External SKU     // Apple/Google/channel adapter identity only
```

Do not collapse these into one SKU or `isPremium` field.

## Entitlement subject

Paid ownership belongs to the authenticated Supabase account principal, not the mutable `Player` profile, email address, device, or store display identity.

This gives restore/account-switch/cross-platform policy one stable subject while leaving Player as gameplay/profile state.

## Source of truth

```text
Store/client transaction evidence
    -> provider-specific backend verification
    -> idempotent CommerceEvent
    -> normalized Entitlement/credit state
    -> AccessEvaluator
```

Client callbacks never grant protected access directly.

Organization/promotional/support grants enter through authorized backend grant operations and use the same normalized entitlement model without fabricating store transactions.

## Offline access

A client may cache a bounded server-issued entitlement snapshot containing only normalized grants and policy metadata.

```text
EntitlementSnapshot
  subject
  issuedAt
  expiresAt/refreshBy
  normalized grants
  policyVersion
  integrity/signature envelope
```

Requirements:

- signing secret remains server-side;
- account identity and expiry are integrity protected;
- snapshot is account-namespaced;
- invalid/expired snapshot fails closed for protected content;
- free gameplay remains usable when entitlement infrastructure is unavailable;
- store services are not contacted on each mission/task transition.

### Not DRM

A signed snapshot proves what the **official client** was authorized to use during its bounded offline window. It does not make an open-source/GPL mobile binary tamper-proof and must not be described as DRM.

A modified client can bypass client-side authorization logic. Therefore:

- server-hosted protected content/assets should require backend authorization before delivery where commercial policy needs protection;
- server-side actions/rewards/organization operations re-check authorization at trusted boundaries where appropriate;
- privileged authority-only Mission data remains server-side where practical;
- offline content already delivered to a device may be copyable on a compromised/modified client;
- the commercial moat remains service/content/brand/operations rather than secrecy of client checks.

The signed snapshot improves official-client correctness, replay bounds, and offline UX; it is not a promise of piracy prevention.

## Portability

ADR-0006 supports account-wide or source-platform-restricted entitlements through explicit `PortabilityPolicy`.

#106 owns the current legal/store/product decision for each SKU. Store adapters implement that decision; they do not infer it.

## Private Events

If the approved first Private Event purchase is one-use/consumable, model it server-side:

```text
verified grant -> ConsumableCredit(1)
               -> atomic consume + create event
               -> event-scoped host entitlement
```

Do not create general virtual currency. #116 is conditional on #106's product/store classification.

## Implementation slices

1. **#112** — shared Product/Entitlement types + deterministic AccessEvaluator.
2. **#113** — backend normalized commerce-event/entitlement service + RLS/idempotency.
3. **#114** — signed bounded offline snapshots + account-scoped cache policy.
4. **#115** — first approved Apple/Google billing adapters after #106 verification.
5. **#116** — conditional Private Event one-time credit/atomic consumption.

All remain post-alpha non-blockers under #90.
