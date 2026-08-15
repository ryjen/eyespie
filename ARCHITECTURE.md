# Eyespie Architecture

## Decision

Eyespie is **backendless-first**. The game engine, authored game data, clue data, image embeddings, matching, and play progress live on the device. A hosted backend is an optional adapter, not part of the core architecture.

The previous cloud-authoritative implementation remains recoverable from `archive/pre-backendless-reboot-2026-08-15` at `50091a631d971c520e48884cfbd15cf15dd7251b`.

## Product architecture

```text
                    Eyespie KMP
                        |
        +---------------+---------------+
        |               |               |
   local identity   local game data   device ML
        |               |               |
 secure storage     SQLDelight/files   MediaPipe
        |               |               |
        +---------------+---------------+
                        |
                   Game engine
                        |
             +----------+----------+
             |                     |
       portable bundle       optional transport
                                   |
                         +---------+---------+
                         |                   |
                    local/peer          cloud adapter
```

The first reboot slice deliberately contains only the smallest portable domain and UI skeleton. Persistence, cryptographic identity, bundle signing, and peer transport are added as explicit vertical slices rather than inherited from the retired backend model.

## Core domain

Core models contain no provider-specific IDs or SDK types.

Initial contracts:

- `PlayerIdentityRepository` — stable local player identity.
- `GameRepository` — local-authoritative game persistence.
- `MatchEngine` — deterministic cosine matching of target and guess embeddings.
- `GameTransport` — optional multiplayer transport capability.
- `CloudSyncAdapter` — optional remote backup/synchronization capability.

Planned contracts:

- `ThingRepository`
- `GameBundleCodec`
- `ClueGenerator`
- `SecureIdentityStore`

## Matching

Matching is local by default:

```text
camera image
    |
MediaPipe image embedding
    |
    +-------------------+
                        |
                target embedding
                        |
                cosine similarity
                        |
                 match / no match
```

No database vector search is required for target-specific gameplay.

Portable/offline games may distribute the target embedding. This means a sufficiently motivated owner of a guesser device can inspect it. That is an explicit product tradeoff, not a security boundary.

For stronger anti-cheat multiplayer, a host-authoritative transport may keep target embeddings on the host and accept only guess embeddings from peers.

## Identity

The default player identity will be device-local and cryptographic:

1. Generate a keypair on first launch.
2. Store private key material using platform secure storage.
3. Derive a stable `PlayerId` from the public key.
4. Sign authored game bundles and authority-relevant peer events.
5. Allow a display name without creating a hosted account.

A future cloud account may link/recover a local identity, but the core domain must not require one.

## Persistence

Local persistence is authoritative. The planned implementation uses SQLDelight for structured game/progress data and device files for larger local artifacts. Repository APIs should expose local semantics directly rather than a network-first/cache-fallback model.

## Portable game format

A future versioned `.eyespie` bundle contains the minimum data required to play offline:

```text
manifest/version
creator public identity
rules/game metadata
things
  clue authority
  target embedding
  matching threshold/version
optional assets
signature
```

Raw target photos do not need to be distributed for matching.

## Optional capabilities

Cloud is allowed only when it provides a product capability that cannot be satisfied locally:

- encrypted backup and cross-device sync;
- public game discovery;
- remote multiplayer/session relay;
- identity recovery/linking;
- opt-in telemetry.

A cloud implementation must sit behind an interface and the app must remain functional when it is absent.

## Platform and ML preservation

The reboot preserves qualified infrastructure that remains valuable:

- Kotlin Multiplatform and Compose;
- Android/iOS app shells;
- `ryjen/mediapipe` Apple artifacts and pod graph;
- Android MediaPipe Tasks dependencies;
- model packaging/provenance assets;
- cross-platform calibration assets.

The old feature/data/backend code is intentionally not carried into the new application source tree.

## Shared framework policy

`hackelia-micrantha/bluebell` and `bluebell-community` remain sources of reusable platform/architecture capabilities. Reintroduce individual abstractions only when the reboot has a concrete need; the core must not depend on a broad framework merely for historical compatibility.

## Delivery sequence

1. Minimal backend-free KMP app + deterministic match engine.
2. SQLDelight local game/Thing/progress persistence.
3. Secure local cryptographic identity.
4. Restore MediaPipe capture/embedding generation behind narrow interfaces.
5. Complete offline create -> clue -> guess -> match vertical slice.
6. Versioned signed `.eyespie` import/export.
7. Optional local/peer host transport.
8. Evaluate optional cloud adapters only from demonstrated product needs.

Each step must keep Android and iOS buildable without backend configuration.
