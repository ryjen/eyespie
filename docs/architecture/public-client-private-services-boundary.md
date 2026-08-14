# Public Client / Private Product-Services Boundary

## Status

**Decision:** Eyespie uses a public-client/private-product-services architecture.

- Public Eyespie client: `ryjen/eyespie`, MPL-2.0.
- Public reusable framework: `hackelia-micrantha/bluebell`, Apache-2.0.
- Public MediaPipe integration/distribution tooling: `ryjen/mediapipe`, under its applicable project/upstream licensing.
- Private product-services/operations: a separate post-alpha boundary for privileged backend, commercial, moderation, anti-abuse, production-operations, and proprietary experiment material where separation has concrete value.

This decision is tracked by #142 and coordinates with #15, #90, #106, and #121.

While #90 remains open, this document defines architecture/backlog only. It does not authorize post-alpha service extraction, billing, Mission, analytics, or alternate-runtime implementation.

## Why the client remains public

The distributed mobile application is not a useful secrecy boundary. A production binary can be inspected, instrumented, or modified regardless of repository visibility.

Security and commercial authority therefore must not depend on hidden client code. In particular:

- privileged operations authenticate and authorize on the server;
- anti-abuse and moderation policy remains server-authoritative where it protects shared/public state;
- entitlement and paid-access authority does not rely on writable local flags;
- client-side digests, signatures, and checks protect correctness and official-client behavior but are not DRM;
- secrets are never embedded in the client or treated as protected merely because source is private.

The public client remains useful for transparency, contribution, portfolio/community value, reproducible builds, and interoperability while MPL-2.0 preserves file-level copyleft for Eyespie-authored client files.

## Repository boundaries

### `ryjen/eyespie` — public MPL-2.0 client

The public repository should own material needed to build, understand, test, and interoperate with the distributed application, including:

- Kotlin Multiplatform application/UI code;
- game/domain contracts and client-side policy boundaries;
- camera and capture integrations;
- on-device MediaPipe inference and model/runtime adapters;
- public protocol/API contracts;
- public-safe backend schema/migrations needed for reproducible development or interoperability;
- release/build/test tooling required to reproduce the client;
- security/privacy architecture that does not expose secrets;
- non-sensitive fixtures and validation data;
- public documentation.

The repository currently embeds `bluebell/`. Until #15 completes extraction, that subtree remains separately Apache-2.0 under `bluebell/LICENSE`; the root MPL-2.0 license does not silently relicense it.

### `hackelia-micrantha/bluebell` — public Apache-2.0 framework

Bluebell should converge on the canonical reusable Kotlin Multiplatform framework/SDK boundary:

- generic application architecture and state-management primitives;
- reusable DI/platform abstractions;
- downloader/cache primitives;
- observability foundations;
- reusable shared UI foundations;
- provider/runtime-neutral framework abstractions.

Bluebell must not encode Eyespie-specific clue semantics, model-provider preference, privacy policy, scoring policy, Supabase schema assumptions, or commercial rules.

#15 owns the bounded extraction from the current local `:bluebell` module into the canonical public Bluebell dependency. Extraction remains non-blocking for #90.

### `ryjen/mediapipe` — public integration/distribution tooling

Keep project-specific MediaPipe build/distribution work public where practical. Eyespie should consume stable artifacts/contracts while retaining exact artifact identity, SBOM, license/NOTICE, and platform validation evidence.

The repository-level license does not replace review of licenses/notices for nested binaries, models, or transitive components in the exact shipped bundle. #121 owns that release-evidence layer.

### Private Eyespie product-services / operations

A private repository/service boundary is appropriate for material that is not required to build or understand the distributed client and for which separation provides an operational or commercial benefit, including as applicable:

- privileged Supabase/Edge Function implementation and service-role automation;
- server-authoritative scoring and anti-abuse logic;
- moderation/admin/support tooling;
- entitlement normalization and commercial policy implementation;
- production deployment configuration and operational runbooks;
- vendor-specific production integration configuration;
- private evaluation corpora/datasets;
- unreleased experiments or proprietary commercial strategy.

This is an IP/operations boundary, not a security control. All privileged behavior still requires explicit authentication, authorization, validation, auditability, and fail-safe defaults.

## Backend classification rule

Do not move code merely because it lives under `supabase/`.

Classify each backend artifact by responsibility:

### May remain public

- schema definitions and migrations needed for reproducible development;
- public protocol contracts;
- RLS/policy definitions whose security depends on correctness rather than secrecy;
- deterministic local-development setup;
- client-compatible test fixtures without sensitive data;
- public-safe server functions where transparency/interoperability is useful.

### Candidate for private product-services

- service-role or privileged operational automation;
- moderation/admin-only logic;
- anti-fraud/anti-abuse internals where disclosure has no interoperability benefit;
- commercial pricing/entitlement policy implementation;
- production-only orchestration and runbooks;
- private datasets/evaluation assets;
- vendor configuration that is operationally sensitive.

No repository may contain plaintext production secrets. Secret values belong in the approved deployment/secret-management path.

Any extraction must preserve migration ordering, deployment provenance, rollback, local development, and CI reproducibility.

## Dependency direction

```text
PUBLIC

Eyespie client (MPL-2.0)
        |
        v
Bluebell KMP framework (Apache-2.0)
        |
        v
MediaPipe / platform dependencies

        | stable authenticated API/contracts
        v

PRIVATE

Eyespie product services / operations
  - privileged backend behavior
  - server-authoritative commercial/safety policy
  - moderation / anti-abuse / admin
  - production operations
```

Rules:

1. The client depends only on stable public contracts and platform/runtime dependencies.
2. Private services implement authenticated APIs/contracts; they do not depend on client UI/application internals.
3. Server-side authorization must treat a modified/untrusted client as normal threat-model input.
4. Product-specific policy stays in Eyespie/service layers, not Bluebell primitives.
5. Separately licensed Mission/content/brand assets are not automatically covered by the client source license.

## Licensing boundary

PR #102 changed the current Eyespie repository root license to MPL-2.0 and added an explicit nested Apache-2.0 license for `bluebell/`.

Current scope:

| Boundary | Current license/status |
| --- | --- |
| Eyespie-authored repository material covered by root license | MPL-2.0 |
| Embedded `bluebell/` subtree | Apache-2.0 |
| Canonical public Bluebell repository | Apache-2.0 |
| `ryjen/mediapipe` root | Apache-2.0, subject to nested/transitive artifact review |
| Third-party dependencies/models/assets | Applicable upstream/component terms |
| Private product services/operations | To be explicitly defined per repository/component; not implied by the client license |
| Eyespie trademarks/brand and separately authored content | Separate policy required |

Previously distributed GPLv3 revisions remain subject to the terms under which recipients obtained those copies. The current license change does not revoke prior grants.

#106 remains the commercial/legal distribution gate and #121 remains the exact-candidate license/NOTICE evidence gate.

## Bluebell repository reconciliation

Historical Bluebell repository intent has drifted: prior documentation described a private-core/public-docs split, while the canonical public `hackelia-micrantha/bluebell` repository now contains the reusable SDK/framework source.

The target model is:

- one canonical public Apache-2.0 Bluebell SDK/framework repository;
- Eyespie consumes it as a versioned dependency or bounded composite-build development dependency;
- stale private Bluebell repositories are archived, repurposed, or documented explicitly rather than maintaining duplicate source authority;
- no `bluebell-community` repository is needed unless a concrete future distribution/community requirement appears.

#15 owns the implementation/extraction details.

## Sequencing

### While #90 is open

Allowed:

- architecture and backlog refinement;
- repository-boundary classification;
- Bluebell API inventory;
- documentation and license-boundary hygiene;
- service/API contract design.

Not allowed solely because of this decision:

- speculative backend extraction;
- billing/entitlement implementation;
- Mission persistence/runtime implementation;
- analytics implementation;
- alternate-runtime experimentation.

### After #90

1. Complete #15 Bluebell extraction in behavior-preserving slices.
2. Inventory current backend artifacts and classify public/private ownership before moving files.
3. Establish the private product-services repository only when there is concrete material to place in it.
4. Move one bounded privileged/operations slice at a time with deployment and rollback evidence.
5. Preserve public client reproducibility and exact release provenance.

## Acceptance principles

The boundary is successful when:

- the public client can be built and tested without production secrets;
- no security property depends on client-source secrecy;
- Bluebell is reusable without Eyespie-specific policy;
- privileged commercial/safety operations remain server-authoritative;
- repository/license scope is explicit rather than inferred;
- exact release-bundle license/NOTICE evidence remains reproducible;
- post-alpha extraction does not destabilize the #90 release path.
