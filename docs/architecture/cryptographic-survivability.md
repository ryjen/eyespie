# Cryptographic Survivability

Status: post-alpha design guidance  
Tracking: issue #342

Eyespie is local-first and backendless by default. Its cryptographic architecture should preserve that property while leaving a clean migration path for future portable private data, peer-to-peer exchange, synchronized state, and device identity.

The Micrantha-wide baseline is tracked in hackelia-micrantha/.github#149.

## Current trust boundary

The current security model remains authoritative:

- core play does not require a hosted account, database, or storage service;
- player private key material belongs in Android Keystore / Apple Keychain or equivalent platform-backed storage;
- private key material must not be placed in portable game bundles;
- data that the local application must consume cannot be treated as secret from a sufficiently motivated owner of that endpoint;
- network capabilities remain optional future adapters subject to explicit review.

This document does not add a backend or network dependency to the alpha.

## Classify confidentiality separately from integrity

Not every Eyespie object needs secrecy.

### Integrity/authenticity-first examples

- portable game rules/content intended to be shared;
- public or player-visible metadata;
- signed bundle manifests;
- content whose main requirement is detecting tampering or identifying the issuer/author.

### Potential confidentiality examples

Future features may introduce:

- private synchronized player state;
- private host-authored game data before reveal;
- private remote/P2P exchange;
- account/recovery material;
- sensitive user-generated content;
- AI inputs that should not be visible to a relay/storage service.

Those cases should state a confidentiality horizon rather than inherit encryption accidentally from a transport.

## Endpoint-owner limitation

If a player device must decrypt and use content, a sufficiently compromised or owner-controlled endpoint can eventually observe that plaintext.

Therefore Eyespie must not claim that encrypting locally usable target embeddings provides a strong anti-cheat boundary.

Where stronger secrecy is needed, the authority that must keep the secret must avoid sending the secret to the untrusted endpoint until disclosure is intended.

## Zero-access applicability

Zero-access/client-side encryption is useful only where there is a service or relay that should not possess plaintext authority.

A future pattern may look like:

~~~text
trusted device
  -> random object key
  -> authenticated encryption
  -> versioned recipient/key envelope
  -> relay / remote storage / portable object
~~~

The relay should not need:

- plaintext;
- the raw object key;
- a durable universal private unwrap key.

This is different from merely enabling encryption at rest on a backend.

It is also different from a zero-knowledge proof.

## Portable object crypto profile

If a portable Eyespie object gains confidentiality or signature requirements, the format should carry an explicit versioned crypto profile rather than embedding one permanent asymmetric algorithm into the domain model.

Conceptually:

~~~text
crypto_profile:
  version
  payload_aead
  recipient_envelope
  signature_profile
  key_id / owner identity reference
~~~

The exact wire format belongs to the owning bundle/spec issue.

The important property is that algorithm/profile migration does not require inventing a new player/game identity.

## Device/player identity

Device identity and long-lived player identity should remain separable.

A platform-backed key may be:

- non-exportable;
- device-specific;
- replaceable after device loss;
- unsuitable as the only durable recovery root.

A future design should distinguish:

~~~text
player / ownership identity
  != one physical device key
  != one crypto algorithm forever
~~~

Recovery and rotation must not require putting raw private key material into a portable game bundle.

## Post-quantum migration

Post-quantum work is a migration concern, not an alpha blocker.

For future long-lived private data or identity:

- keep crypto suites/versioning explicit;
- identify classical asymmetric dependencies in signing/key exchange/wrapping;
- adopt standardized PQ or reviewed hybrid profiles when supported by the target platform/protocol ecosystem;
- make downgrade behavior explicit;
- avoid custom KEM/signature constructions.

The important question is whether the protected object's confidentiality/authenticity lifetime exceeds the useful lifetime of its current cryptographic profile.

## Peer-to-peer relationship

Issue #300 owns nearby peer-to-peer transfer.

A future private P2P profile should distinguish:

- discovery/session authentication;
- key establishment;
- object confidentiality;
- object authenticity/signature;
- replay/freshness;
- device/player identity.

P2P transport encryption alone does not automatically protect a durable object from harvest-now/decrypt-later exposure if the object remains recoverable through a classical-only durable envelope.

## Bundle relationship

Issues #167 and #173 own the signed portable .eyespie bundle design and codec.

This guidance should not destabilize the current bundle work.

When confidentiality is introduced later:

- version the crypto profile;
- preserve existing object identity where practical;
- keep private keys outside the bundle;
- make profile migration explicit;
- test old/new profile coexistence and unsupported-profile rejection.

## AI features

If future AI-assisted features process encrypted/private Eyespie data:

- decrypt only inside the explicitly trusted boundary;
- prefer per-object/session authority rather than a reusable root key;
- do not expose a universal decrypt capability to a model runtime;
- minimize plaintext persistence/logging;
- preserve non-secret provenance needed to explain access;
- do not claim that deleting a model/session removes copies already exported to third parties.

## Crypto-erasure

Key destruction may make retained ciphertext unusable only if all required recovery paths remain under Eyespie's control.

A valid future claim must account for:

- device grants;
- recovery material;
- exported backups;
- relay copies;
- already-decrypted plaintext.

Do not claim that attacker-held ciphertext can detect cryptanalysis or self-destruct.

## Delivery order

1. Keep alpha backendless and unchanged.
2. Define confidentiality/integrity classes for future portable/private data.
3. Version any new cryptographic profile.
4. Define device/player key rotation and recovery.
5. Add PQ/hybrid profiles only when supported by reviewed platform/protocol implementations.
6. Add conformance fixtures before enabling migration/downgrade behavior.

## Non-goals

This architecture does not:

- require a backend;
- block the current alpha;
- make local embeddings secret from the device owner;
- define a custom KMS/KEM/signature scheme;
- predict Q-day;
- promise self-destructing ciphertext;
- make an AI model a key authority.
