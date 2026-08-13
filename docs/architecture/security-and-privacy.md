# Security and Privacy Architecture

The normative closed-alpha privacy/security baseline is:

- [`docs/security/privacy-threat-model.md`](../security/privacy-threat-model.md)

This architecture note keeps the major cross-cutting boundaries visible from the architecture documentation without duplicating the full threat analysis.

## Core boundary

Eyespie processes high-sensitivity camera, exact-location, embedding, generated-answer, account, and model data. Architecture must therefore preserve:

- transient live camera frames;
- app-private, bounded explicit still-capture retention;
- local-first inference;
- current-operation-only model context;
- explicit model/version compatibility;
- least-privilege backend/storage projections;
- short-lived bearer capabilities rather than persisted signed URLs;
- restricted embeddings, exact location, hidden proof, auth identity, and private paths;
- fail-closed model integrity and authorization;
- truthful separation between app uploads, local inference, vendor telemetry, diagnostics, and future product analytics.

## Primary data flow

```text
Camera / location
      |
      v
Eyespie app
  |   |   \
  |   |    -> local MediaPipe / GenAI
  |   |          |
  |   |          -> bounded semantic / embedding result
  |   |
  |   -> app-private temporary capture / bounded cache
  |
  -> Supabase Auth / Postgres-RPC / Storage
        only through purpose-specific authorization and minimum projections
```

Downloaded model artifacts cross a separate supply-chain boundary and must pass #9 SHA-256 verification before initialization.

## Current release mitigations

The threat model identified concrete current-state gaps:

- #122 — restrict Thing/image/location/embedding/proof exposure and stop persisting 365-day signed image URLs;
- #123 — isolate GenAI request/session context and remove raw clue/answer logging;
- #124 — minimize local raw-capture retention and remove unintended Android MediaStore copies;
- #125 — verify exact MediaPipe runtime telemetry/disclosure behavior;
- #126 — separate public Player profile fields from private account/location state;
- #91 — complete canonical model/version-aware embedding behavior.

These are release/security work, not post-alpha monetization work.

## Semantic engine

The semantic engine architecture in [`semantic-game-engine.md`](semantic-game-engine.md) remains local-first and denies remote execution by default. Any future remote reasoning route requires:

1. declared minimum data capabilities;
2. policy authorization;
3. consent where required;
4. payload minimization;
5. bounded provenance that does not log the sensitive payload.

A remote provider must never inherit access to raw images, precise location, OCR text, faces, embeddings, or historical scene context merely because local inference is unavailable.

## Offline

Offline caching is not permission to retain sensitive state indefinitely. #16 must define:

- account namespace/isolation;
- TTL/eviction for sensitive cached state;
- raw capture retry lifetime;
- logout/account-switch behavior;
- reconnect reconciliation;
- behavior when data cannot be safely validated or authorized while offline.

The existing `PendingCapture` representation is not approval for unbounded offline storage of image paths, location, clues, and embeddings.

## AR / spatial

#8 remains later-phase and optional. The privacy default is:

- no raw-frame persistence;
- no room/world-map persistence;
- no camera-pose trail persistence;
- no persistent spatial anchors by default;
- no spatial upload without an explicit future purpose, consent/authorization, and retention policy;
- camera-only fallback remains first-class.

Spatial maps/anchors are restricted place data and require a future extension of the baseline threat model before implementation.

## Commercial extension

#107 extends this baseline only after the free/core protections are established. It owns additional incentives/surfaces such as geofenced public UGC, paid-content copying, entitlement/payment abuse, moderation, physical-location publishing, and future creator-payout fraud.

Commercial architecture must not weaken the free/core data boundaries documented here.
